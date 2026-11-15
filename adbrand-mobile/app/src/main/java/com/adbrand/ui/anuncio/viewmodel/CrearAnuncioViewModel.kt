package com.adbrand.ui.anuncio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AnuncioRepository
import com.adbrand.data.repository.AnuncioRepositoryRemoto
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.RedSocial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAXIMO_OFERTA = 300

// HU 10, 12 y 13: estado del formulario y del anuncio generado.
data class CrearAnuncioEstado(
    val oferta: String = "",
    val redSocial: RedSocial = RedSocial.INSTAGRAM,
    val errorOferta: String? = null,
    val anuncio: AnuncioGenerado? = null,
    val aviso: String? = null,
    val faltaPerfil: Boolean = false,
    val generando: Boolean = false,
    val regenerando: Boolean = false,
    val copiado: Boolean = false
) {
    val cargando: Boolean
        get() = generando || regenerando
}

class CrearAnuncioViewModel(
    private val repositorio: AnuncioRepository = AnuncioRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(CrearAnuncioEstado())
    val estado: StateFlow<CrearAnuncioEstado> = _estado.asStateFlow()

    fun actualizarOferta(valor: String) {
        _estado.update { it.copy(oferta = valor, errorOferta = null) }
    }

    fun actualizarRedSocial(red: RedSocial) {
        _estado.update { it.copy(redSocial = red) }
    }

    fun generar() {
        val actual = _estado.value
        if (actual.cargando) return

        val errorLocal = validar(actual.oferta)
        if (errorLocal != null) {
            _estado.update {
                it.copy(errorOferta = errorLocal, aviso = null, faltaPerfil = false)
            }
            return
        }

        viewModelScope.launch {
            _estado.update {
                it.copy(
                    generando = true,
                    errorOferta = null,
                    aviso = null,
                    faltaPerfil = false,
                    copiado = false
                )
            }
            when (val resultado = repositorio.generar(actual.oferta.trim(), actual.redSocial)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(generando = false, anuncio = resultado.dato)
                }

                is Resultado.Error -> _estado.update { estado ->
                    when {
                        // Criterio 1: sin perfil registrado no se puede generar.
                        resultado.codigoError == "PERFIL_NO_ENCONTRADO" ->
                            estado.copy(generando = false, faltaPerfil = true)

                        // Un 400 trae el detalle por campo.
                        resultado.codigo == 400 && resultado.campos.isNotEmpty() ->
                            estado.copy(generando = false, errorOferta = resultado.campos["oferta"])

                        else -> estado.copy(generando = false, aviso = resultado.mensaje)
                    }
                }
            }
        }
    }

    // HU 12: regenerar con los mismos parámetros del anuncio original.
    fun regenerar() {
        val actual = _estado.value
        val anuncio = actual.anuncio ?: return
        val id = anuncio.id ?: return
        if (actual.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(regenerando = true, aviso = null, copiado = false) }
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

    fun marcarCopiado() {
        _estado.update { it.copy(copiado = true) }
    }

    private fun validar(oferta: String): String? = when {
        oferta.isBlank() -> "Escribe la oferta que quieres anunciar."
        oferta.length > MAXIMO_OFERTA -> "Máximo $MAXIMO_OFERTA caracteres."
        else -> null
    }
}
