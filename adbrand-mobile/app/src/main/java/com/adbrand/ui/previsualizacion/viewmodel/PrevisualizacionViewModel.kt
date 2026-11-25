package com.adbrand.ui.previsualizacion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PrevisualizacionRepository
import com.adbrand.data.repository.PrevisualizacionRepositoryRemoto
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
import com.adbrand.ui.previsualizacion.recorte.AjusteRecorte
import com.adbrand.ui.previsualizacion.recorte.Recorte
import com.adbrand.domain.model.KitMarca
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 45 y 46: estado de la previsualización.
// formato: el elegido (cuadrado, vertical u horizontal); publicacion: el anuncio abierto en ese formato con su imagen.
data class PrevisualizacionEstado(
    val cargando: Boolean = false,
    val kit: KitMarca? = null,
    val anuncios: List<Anuncio> = emptyList(),
    val error: String? = null,
    val cargado: Boolean = false,
    val formato: FormatoVista = FormatoVista(),
    val publicacion: Anuncio? = null,
    val imagen: String? = null,
    val cargandoImagen: Boolean = false,
    val errorImagen: String? = null,
    // HU 48: zoom y posición del recorte dentro del marco, y el aviso después de guardar/compartir.
    val ajuste: AjusteRecorte = AjusteRecorte(),
    val mensajeCompartir: String? = null,
    // Listo para publicar: el texto con hashtags y el enlace de WhatsApp (null mientras llega o si falla).
    val textoPublicar: String? = null,
    // HU 64: lo que conviene corregir según INDECOPI (titulo, detalle).
    val observacionesLegales: List<Pair<String, String>> = emptyList(),
    // HU 65: el video se arma en el servidor (unos segundos).
    val creandoVideo: Boolean = false,
    // Ajuste 14: el anuncio diseñado (plantilla de la opción elegida) en el formato actual. null si no llegó.
    val diseno: ByteArray? = null,
    // "Subir a mi estado": se pide la imagen vertical y se abre WhatsApp.
    val subiendoEstado: Boolean = false,
    // El video es de los planes de pago: con el plan gratis el botón sale con candado.
    val videoDisponible: Boolean = true
)

class PrevisualizacionViewModel(
    private val repositorio: PrevisualizacionRepository = PrevisualizacionRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(PrevisualizacionEstado())
    val estado: StateFlow<PrevisualizacionEstado> = _estado.asStateFlow()

    fun cargar() = cargarEn(_estado.value.formato.codigo)

    // HU 46, criterio 1: el usuario elige el formato y los anuncios se vuelven a pedir ajustados a ese formato.
    fun cambiarFormato(codigo: String) {
        if (codigo == _estado.value.formato.codigo) return
        cargarEn(codigo)
    }

    private fun cargarEn(codigo: String) {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.obtener(codigo)) {
                is Resultado.Exito -> {
                    _estado.update { actual ->
                    val anuncios = resultado.dato.anuncios
                    actual.copy(
                        cargando = false,
                        cargado = true,
                        kit = resultado.dato.kit,
                        anuncios = anuncios,
                        formato = resultado.dato.formato,
                        // El marco cambió de forma: el recorte vuelve al original.
                        ajuste = if (resultado.dato.formato.codigo != actual.formato.codigo) AjusteRecorte() else actual.ajuste,
                        // Si hay un anuncio abierto, se actualiza con su texto ajustado al nuevo formato.
                        publicacion = actual.publicacion?.let { abierto -> anuncios.find { it.id == abierto.id } ?: abierto },
                        diseno = if (resultado.dato.formato.codigo != actual.formato.codigo) null else actual.diseno
                    )
                    }
                    // El diseño del anuncio abierto se vuelve a pedir en el formato nuevo.
                    _estado.value.publicacion?.id?.let { id -> if (_estado.value.diseno == null) cargarDiseno(id) }
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, cargado = true, error = resultado.mensaje)
                }
            }
        }
    }

    // HU 46, criterio 1: abre el anuncio como se verá publicado y pide su imagen.
    fun abrirPublicacion(anuncio: Anuncio) {
        _estado.update {
            it.copy(
                publicacion = anuncio,
                imagen = null,
                ajuste = AjusteRecorte(),
                mensajeCompartir = null,
                errorImagen = null,
                cargandoImagen = anuncio.id != null,
                textoPublicar = null,
                observacionesLegales = emptyList(),
                diseno = null
            )
        }
        val id = anuncio.id ?: return
        cargarDiseno(id)
        viewModelScope.launch {
            val listo = repositorio.listoParaPublicar(id)
            if (listo is Resultado.Exito && _estado.value.publicacion?.id == id) {
                _estado.update {
                    it.copy(
                        textoPublicar = listo.dato.texto,
                        observacionesLegales = listo.dato.observaciones,
                        videoDisponible = listo.dato.videoDisponible
                    )
                }
            }
        }
        viewModelScope.launch {
            val resultado = repositorio.imagen(id)
            // Si mientras tanto se cerró o se abrió otro anuncio, no se pisa.
            if (_estado.value.publicacion?.id != id) return@launch
            _estado.update {
                when (resultado) {
                    is Resultado.Exito -> it.copy(cargandoImagen = false, imagen = resultado.dato)
                    is Resultado.Error -> it.copy(cargandoImagen = false, errorImagen = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarPublicacion() {
        _estado.update {
            it.copy(
                publicacion = null, imagen = null, cargandoImagen = false, errorImagen = null, ajuste = AjusteRecorte(),
                mensajeCompartir = null, diseno = null
            )
        }
    }

    // HU 48, criterio 1: pellizco (zoom) y arrastre dentro del marco del formato. Las medidas las da la pantalla.
    fun ajustar(zoom: Float, arrastreX: Float, arrastreY: Float, anchoMarco: Float, anchoImagen: Int, altoImagen: Int) {
        _estado.update { actual ->
            val formato = actual.formato
            val proporcion = if (formato.alto > 0) formato.ancho.toFloat() / formato.alto else 1f
            actual.copy(
                ajuste = Recorte.aplicarGesto(actual.ajuste, zoom, arrastreX, arrastreY, anchoMarco, anchoImagen, altoImagen, proporcion),
                mensajeCompartir = null
            )
        }
    }

    // HU 48, criterio 2: vuelve al tamaño y la posición originales.
    fun restablecer() {
        _estado.update { it.copy(ajuste = AjusteRecorte(), mensajeCompartir = null) }
    }

    fun avisar(mensaje: String) = _estado.update { it.copy(mensajeCompartir = mensaje) }

    // Ajuste 14: pide el anuncio diseñado en el formato actual. Si falla, se sigue viendo el diseño de la app.
    private fun cargarDiseno(id: Long) {
        val formato = _estado.value.formato.codigo
        viewModelScope.launch {
            val resultado = repositorio.imagenFinal(id, formato)
            if (_estado.value.publicacion?.id != id || _estado.value.formato.codigo != formato) return@launch
            if (resultado is Resultado.Exito) _estado.update { it.copy(diseno = resultado.dato) }
        }
    }

    // "Subir a mi estado": el anuncio en vertical (como se ven los estados) y, cuando llega, se abre WhatsApp.
    fun subirAEstado(alListo: (ByteArray) -> Unit) {
        val id = _estado.value.publicacion?.id ?: return
        if (_estado.value.subiendoEstado) return
        viewModelScope.launch {
            _estado.update { it.copy(subiendoEstado = true, mensajeCompartir = null) }
            when (val resultado = repositorio.imagenFinal(id, "VERTICAL")) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(subiendoEstado = false) }
                    alListo(resultado.dato)
                }
                is Resultado.Error -> _estado.update { it.copy(subiendoEstado = false, mensajeCompartir = resultado.mensaje) }
            }
        }
    }

    // HU 65: pide el video del anuncio abierto y, cuando llega, la pantalla lo comparte ("alListo").
    fun crearVideo(alListo: (ByteArray) -> Unit) {
        val id = _estado.value.publicacion?.id ?: return
        if (_estado.value.creandoVideo) return
        viewModelScope.launch {
            _estado.update { it.copy(creandoVideo = true, mensajeCompartir = null) }
            when (val resultado = repositorio.video(id)) {
                is Resultado.Exito -> {
                    _estado.update { it.copy(creandoVideo = false) }
                    alListo(resultado.dato)
                }
                is Resultado.Error -> _estado.update { it.copy(creandoVideo = false, mensajeCompartir = resultado.mensaje) }
            }
        }
    }

    // HU 48: guarda la imagen recortada en la galería (la pantalla hace el guardado y avisa si funcionó).
    fun guardar(guardarEnGaleria: () -> Boolean) {
        val guardada = runCatching { guardarEnGaleria() }.getOrDefault(false)
        _estado.update {
            it.copy(
                mensajeCompartir = if (guardada) "Imagen guardada en tu galería (carpeta AdBrand)."
                else "No se pudo guardar la imagen en la galería. Intenta de nuevo."
            )
        }
    }

    // HU 47: C1, se genera la imagen del anuncio con la marca y se abre el menú de compartir; queda registrado en el backend.
    // C2: si la imagen no se pudo generar, se muestra un mensaje y NO se abre el menú.
    fun <T : Any> compartir(generar: () -> T?, abrirMenu: (T) -> Unit) {
        val archivo = runCatching { generar() }.getOrNull()
        if (archivo == null) {
            _estado.update { it.copy(mensajeCompartir = MENSAJE_NO_SE_PUDO_COMPARTIR) }
            return
        }
        val abierto = runCatching { abrirMenu(archivo) }.isSuccess
        if (!abierto) {
            _estado.update { it.copy(mensajeCompartir = "No se pudo abrir el menú para compartir. Intenta de nuevo.") }
            return
        }
        _estado.update { it.copy(mensajeCompartir = null) }
        val id = _estado.value.publicacion?.id ?: return
        val formato = _estado.value.formato.codigo
        // Si el registro falla (por ejemplo, sin internet), el anuncio igual se compartió: no se molesta al usuario.
        viewModelScope.launch { repositorio.registrarCompartido(id, formato) }
    }

    companion object {
        const val MENSAJE_NO_SE_PUDO_COMPARTIR = "No se pudo generar la imagen del anuncio para compartirla. Intenta de nuevo."
    }
}
