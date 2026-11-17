package com.adbrand.ui.previsualizacion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PrevisualizacionRepository
import com.adbrand.data.repository.PrevisualizacionRepositoryRemoto
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
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
    val errorImagen: String? = null
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
                is Resultado.Exito -> _estado.update { actual ->
                    val anuncios = resultado.dato.anuncios
                    actual.copy(
                        cargando = false,
                        cargado = true,
                        kit = resultado.dato.kit,
                        anuncios = anuncios,
                        formato = resultado.dato.formato,
                        // Si hay un anuncio abierto, se actualiza con su texto ajustado al nuevo formato.
                        publicacion = actual.publicacion?.let { abierto -> anuncios.find { it.id == abierto.id } ?: abierto }
                    )
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
                errorImagen = null,
                cargandoImagen = anuncio.id != null
            )
        }
        val id = anuncio.id ?: return
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
        _estado.update { it.copy(publicacion = null, imagen = null, cargandoImagen = false, errorImagen = null) }
    }
}
