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
    val copiado: Boolean = false
) {
    val cargando: Boolean
        get() = generando || regenerando || eligiendoId != null || retocando

    val hayOpciones: Boolean
        get() = opciones.isNotEmpty()
}

class CrearAnuncioViewModel(
    private val repositorio: AnuncioRepository = AnuncioRepositoryRemoto(),
    private val opcionesRepositorio: AnuncioOpcionesRepository = AnuncioOpcionesRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(CrearAnuncioEstado())
    val estado: StateFlow<CrearAnuncioEstado> = _estado.asStateFlow()

    fun actualizarOferta(valor: String) {
        _estado.update { it.copy(oferta = valor, errorOferta = null) }
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
            when (val resultado = opcionesRepositorio.generar(actual.oferta.trim(), actual.redSocial)) {
                is Resultado.Exito -> _estado.update { estado ->
                    val opciones = resultado.dato.opciones
                    estado.copy(
                        generando = false,
                        opciones = opciones,
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
                is Resultado.Exito -> _estado.update { estado ->
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

                is Resultado.Error -> _estado.update { estado ->
                    estado.conError(resultado)
                }
            }
        }
    }

    // HU 12: regenerar el texto del anuncio elegido; la imagen se mantiene.
    fun regenerar() {
        val actual = _estado.value
        val anuncio = actual.anuncio ?: return
        val id = anuncio.id ?: return
        if (actual.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(regenerando = true, aviso = null, mensajeExito = null, copiado = false) }
            when (val resultado = repositorio.regenerar(id)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(regenerando = false, anuncio = resultado.dato)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(
                        regenerando = false,
                        aviso = if (resultado.codigo == 404) {
                            "El contenido original ya no existe."
                        } else {
                            resultado.mensaje
                        }
                    )
                }
            }
        }
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
