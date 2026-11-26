package com.adbrand.ui.anuncio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AnuncioOpcionesRepository
import com.adbrand.data.repository.AnuncioOpcionesRepositoryRemoto
import com.adbrand.data.repository.AnuncioRepository
import com.adbrand.data.repository.AnuncioRepositoryRemoto
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.OpcionAnuncio
import com.adbrand.domain.model.RedSocial
import com.adbrand.domain.model.VersionesAnuncio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAXIMO_OFERTA = 300
const val MAXIMO_INSTRUCCION = 300

// HU 10, 12 y 13 + IA de imágenes: estado del formulario, las 4 opciones con
// imagen, el anuncio elegido y el historial de retoques.
data class CrearAnuncioEstado(
    val oferta: String = "",
    val redSocial: RedSocial = RedSocial.INSTAGRAM,
    val errorOferta: String? = null,
    val opciones: List<OpcionAnuncio> = emptyList(),
    val eligiendoId: Long? = null,
    val opcionElegida: OpcionAnuncio? = null,
    val anuncio: AnuncioGenerado? = null,
    val historial: VersionesAnuncio? = null,
    val instruccion: String = "",
    val errorInstruccion: String? = null,
    val retocando: Boolean = false,
    val aviso: String? = null,
    val mensajeExito: String? = null,
    val faltaPerfil: Boolean = false,
    val generando: Boolean = false,
    val regenerando: Boolean = false,
    val copiado: Boolean = false,
    val propuesta: AnuncioGenerado? = null,
    val errorRegeneracion: String? = null,
    // HU 67: foto del producto (opcional) y el aviso si se usó tal cual cuando se pidió una escena.
    val foto: String? = null,
    val errorFoto: String? = null,
    val avisoFoto: String? = null
) {
    val cargando: Boolean
        get() = generando || regenerando || eligiendoId != null || retocando

    val hayOpciones: Boolean
        get() = opciones.isNotEmpty()
}

private val TIPOS_FOTO = setOf("image/jpeg", "image/png")
private const val TAMANO_MAXIMO_FOTO = 5 * 1024 * 1024

class CrearAnuncioViewModel(
    private val repositorio: AnuncioRepository = AnuncioRepositoryRemoto(),
    private val opcionesRepositorio: AnuncioOpcionesRepository = AnuncioOpcionesRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(CrearAnuncioEstado())
    val estado: StateFlow<CrearAnuncioEstado> = _estado.asStateFlow()

    fun actualizarOferta(valor: String) {
        _estado.update { it.copy(oferta = valor, errorOferta = null) }
    }

    // HU 67: la foto de lo que vende (JPG o PNG de hasta 5 MB). Se usa tal cual o, si el pedido dice dónde
    // ponerla, la IA arma esa escena con el producto en 2 de las opciones (planes de pago).
    fun ponerFoto(tipo: String?, bytes: ByteArray?) {
        val error = when {
            bytes == null -> "No pudimos leer tu foto. Prueba con otra."
            tipo !in TIPOS_FOTO -> "Sube tu foto en JPG o PNG."
            bytes.size > TAMANO_MAXIMO_FOTO -> "La foto puede pesar hasta 5 MB."
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(errorFoto = error) }
            return
        }
        val dataUrl = "data:$tipo;base64," + java.util.Base64.getEncoder().encodeToString(bytes)
        _estado.update { it.copy(foto = dataUrl, errorFoto = null) }
    }

    fun quitarFoto() {
        _estado.update { it.copy(foto = null, errorFoto = null) }
    }

    fun actualizarRedSocial(red: RedSocial) {
        _estado.update { it.copy(redSocial = red) }
    }

    // Paso 1: la IA propone 4 anuncios (texto + imagen) para que el dueño elija.
    fun generar() {
        val actual = _estado.value
        if (actual.cargando) return

        val errorLocal = validar(actual.oferta)
        if (errorLocal != null) {
            _estado.update {
                it.copy(errorOferta = errorLocal, aviso = null, mensajeExito = null, faltaPerfil = false)
            }
            return
        }

        viewModelScope.launch {
            _estado.update {
                it.copy(
                    generando = true,
                    errorOferta = null,
                    aviso = null,
                    mensajeExito = null,
                    faltaPerfil = false,
                    copiado = false,
                    opciones = emptyList(),
                    anuncio = null,
                    opcionElegida = null,
                    historial = null,
                    instruccion = "",
                    errorInstruccion = null
                )
            }
            when (val resultado = opcionesRepositorio.generar(actual.oferta.trim(), actual.redSocial, actual.foto)) {
                is Resultado.Exito -> _estado.update { estado ->
                    val opciones = resultado.dato.opciones
                    estado.copy(
                        generando = false,
                        opciones = opciones,
                        avisoFoto = resultado.dato.avisoFoto,
                        aviso = if (opciones.isEmpty()) {
                            "La IA no devolvió propuestas esta vez. Intenta de nuevo."
                        } else {
                            null
                        }
                    )
                }

                is Resultado.Error -> _estado.update { estado -> estado.conError(resultado) }
            }
        }
    }

    // Paso 2: la opción elegida se guarda como anuncio (pasa por el filtro de la HU 13).
    fun elegir(opcionId: Long) {
        val actual = _estado.value
        if (actual.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(eligiendoId = opcionId, aviso = null, mensajeExito = null) }
            when (val resultado = opcionesRepositorio.elegir(opcionId)) {
                is Resultado.Exito -> {
                    _estado.update { estado ->
                        estado.copy(
                            eligiendoId = null,
                            anuncio = resultado.dato.anuncio,
                            opcionElegida = resultado.dato.opcion,
                            opciones = estado.opciones.map { opcion ->
                                opcion.copy(elegida = opcion.id == opcionId)
                            },
                            mensajeExito = "Guardamos tu anuncio con la imagen elegida."
                        )
                    }
                    // Se pide el historial para mostrar el límite de retoques del mes.
                    when (val versiones = opcionesRepositorio.versiones(opcionId)) {
                        is Resultado.Exito -> _estado.update { it.copy(historial = versiones.dato) }
                        is Resultado.Error -> Unit
                    }
                }

                is Resultado.Error -> _estado.update { estado ->
                    estado.conError(resultado)
                }
            }
        }
    }

    // IA de imágenes: retocar la imagen elegida. Cada instrucción crea una versión nueva.
    fun retocar() {
        val actual = _estado.value
        val opcion = actual.opcionElegida ?: return
        if (actual.cargando) return

        val errorLocal = validarInstruccion(actual.instruccion)
        if (errorLocal != null) {
            _estado.update { it.copy(errorInstruccion = errorLocal) }
            return
        }

        viewModelScope.launch {
            _estado.update {
                it.copy(retocando = true, errorInstruccion = null, aviso = null, mensajeExito = null)
            }
            when (val resultado = opcionesRepositorio.retocar(opcion.id, actual.instruccion.trim())) {
                is Resultado.Exito -> _estado.update { estado ->
                    estado.copy(
                        retocando = false,
                        historial = resultado.dato,
                        opcionElegida = estado.opcionElegida?.copy(imagen = resultado.dato.imagenActual, diseno = null),
                        instruccion = "",
                        mensajeExito = "La IA retocó tu imagen. Puedes seguir ajustándola."
                    )
                }

                is Resultado.Error -> _estado.update { estado -> estado.conErrorRetoque(resultado) }
            }
        }
    }

    // Historial: volver a una versión anterior sin gastar otro retoque.
    fun volverAVersion(numero: Int) {
        val actual = _estado.value
        val opcion = actual.opcionElegida ?: return
        if (actual.cargando) return
        if (actual.historial?.versionActual == numero) return

        viewModelScope.launch {
            _estado.update { it.copy(retocando = true, aviso = null, mensajeExito = null) }
            when (val resultado = opcionesRepositorio.usarVersion(opcion.id, numero)) {
                is Resultado.Exito -> _estado.update { estado ->
                    estado.copy(
                        retocando = false,
                        historial = resultado.dato,
                        opcionElegida = estado.opcionElegida?.copy(imagen = resultado.dato.imagenActual, diseno = null),
                        mensajeExito = "Ahora se usa la versión $numero de tu imagen."
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(retocando = false, aviso = resultado.mensaje)
                }
            }
        }
    }

    // HU 55: "Nueva versión" sobre el anuncio elegido. La IA redacta una versión
    // nueva y se queda esperando la comparación con la anterior; el usuario decide
    // cuál conservar.
    fun regenerar() {
        val actual = _estado.value
        val anuncio = actual.anuncio ?: return
        val id = anuncio.id ?: return
        if (actual.cargando) return

        viewModelScope.launch {
            _estado.update {
                it.copy(
                    regenerando = true,
                    aviso = null,
                    mensajeExito = null,
                    copiado = false,
                    errorRegeneracion = null,
                    propuesta = null
                )
            }
            when (val resultado = repositorio.regenerar(id)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        regenerando = false,
                        propuesta = resultado.dato,
                        mensajeExito = "La IA redactó una versión nueva."
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(regenerando = false, errorRegeneracion = resultado.mensaje)
                }
            }
        }
    }

    // HU 55: la versión nueva pasa a ser la que se usa, sin perder la anterior.
    fun aceptarPropuesta() {
        val propuesta = _estado.value.propuesta ?: return
        _estado.update {
            it.copy(propuesta = null, anuncio = propuesta, mensajeExito = "Ahora usamos la versión que te gustó.")
        }
    }

    // HU 55: descartar la versión nueva y volver a la anterior.
    fun descartarPropuesta() {
        _estado.update { it.copy(propuesta = null) }
    }

    fun actualizarInstruccion(valor: String) {
        _estado.update { it.copy(instruccion = valor, errorInstruccion = null) }
    }

    fun marcarCopiado() {
        _estado.update { it.copy(copiado = true) }
    }

    fun limpiarMensaje() {
        _estado.update { it.copy(aviso = null, mensajeExito = null) }
    }

    private fun validar(oferta: String): String? = when {
        oferta.isBlank() -> "Escribe la oferta que quieres anunciar."
        oferta.length > MAXIMO_OFERTA -> "Máximo $MAXIMO_OFERTA caracteres."
        else -> null
    }

    private fun validarInstruccion(instruccion: String): String? = when {
        instruccion.isBlank() -> "Escribe qué quieres cambiar de la imagen."
        instruccion.length > MAXIMO_INSTRUCCION -> "Máximo $MAXIMO_INSTRUCCION caracteres."
        else -> null
    }

    private fun CrearAnuncioEstado.conErrorRetoque(resultado: Resultado.Error): CrearAnuncioEstado {
        val errorCampo = resultado.campos["instruccion"]
        return when {
            errorCampo != null -> copy(retocando = false, errorInstruccion = errorCampo)
            else -> copy(retocando = false, aviso = resultado.mensaje)
        }
    }

    private fun CrearAnuncioEstado.conError(
        resultado: Resultado.Error
    ): CrearAnuncioEstado = when {
        resultado.codigoError == "PERFIL_NO_ENCONTRADO" ->
            copy(generando = false, eligiendoId = null, faltaPerfil = true)

        resultado.codigo == 400 && resultado.campos.isNotEmpty() ->
            copy(
                generando = false,
                eligiendoId = null,
                errorOferta = resultado.campos["oferta"]
            )

        else -> copy(
            generando = false,
            eligiendoId = null,
            aviso = resultado.mensaje
        )
    }
}
