package com.adbrand.ui.perfil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.CuentaRepository
import com.adbrand.data.repository.CuentaRepositoryRemoto
import com.adbrand.data.repository.PerfilRepository
import com.adbrand.data.repository.PerfilRepositoryRemoto
import com.adbrand.domain.model.Empresa
import com.adbrand.domain.model.PerfilNegocio
import com.adbrand.domain.model.Tono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 44: estado de la pantalla "Perfil del negocio".
data class PerfilNegocioEstado(
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val nombreComercial: String = "",
    val rubro: String = "",
    val publicoObjetivo: String = "",
    val tono: Tono = Tono.CERCANO,
    val descripcion: String = "",
    val correo: String = "",
    val telefono: String = "",
    val errorNombre: String? = null,
    val errorRubro: String? = null,
    val errorPublico: String? = null,
    val errorTelefono: String? = null,
    val error: String? = null,
    val mensaje: String? = null
)

class PerfilNegocioViewModel(
    private val perfiles: PerfilRepository = PerfilRepositoryRemoto(),
    private val cuentas: CuentaRepository = CuentaRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(PerfilNegocioEstado())
    val estado: StateFlow<PerfilNegocioEstado> = _estado.asStateFlow()

    private var empresaActual: Empresa? = null

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, mensaje = null) }

            when (val perfil = perfiles.obtener()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        nombreComercial = perfil.dato.nombreComercial,
                        rubro = perfil.dato.rubro,
                        publicoObjetivo = perfil.dato.publicoObjetivo,
                        tono = perfil.dato.tono,
                        descripcion = perfil.dato.descripcion
                    )
                }

                is Resultado.Error -> {
                    _estado.update { it.copy(cargando = false, error = perfil.mensaje) }
                    return@launch
                }
            }

            // El correo y el teléfono viven en la cuenta de la empresa (HU 41).
            when (val cuenta = cuentas.obtener()) {
                is Resultado.Exito -> {
                    empresaActual = cuenta.dato
                    _estado.update { it.copy(correo = cuenta.dato.correo, telefono = cuenta.dato.telefono) }
                }

                is Resultado.Error -> Unit
            }
        }
    }

    fun actualizarNombreComercial(valor: String) {
        _estado.update { it.copy(nombreComercial = valor, errorNombre = null, mensaje = null) }
    }

    fun actualizarRubro(valor: String) {
        _estado.update { it.copy(rubro = valor, errorRubro = null, mensaje = null) }
    }

    fun actualizarPublicoObjetivo(valor: String) {
        _estado.update { it.copy(publicoObjetivo = valor, errorPublico = null, mensaje = null) }
    }

    fun actualizarTono(valor: Tono) {
        _estado.update { it.copy(tono = valor, mensaje = null) }
    }

    fun actualizarDescripcion(valor: String) {
        _estado.update { it.copy(descripcion = valor, mensaje = null) }
    }

    fun actualizarTelefono(valor: String) {
        _estado.update { it.copy(telefono = valor.filter(Char::isDigit).take(9), errorTelefono = null, mensaje = null) }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando) return

        val errorNombre = if (actual.nombreComercial.isBlank()) "Ingresa el nombre comercial." else null
        val errorRubro = if (actual.rubro.isBlank()) "Selecciona el rubro." else null
        val errorPublico = if (actual.publicoObjetivo.isBlank()) "Describe a tu público objetivo." else null
        val errorTelefono = if (actual.telefono.isNotBlank() && !TELEFONO_VALIDO.matches(actual.telefono)) {
            "Escribe un celular de 9 dígitos que empiece en 9, o un fijo de 7 dígitos."
        } else null

        if (errorNombre != null || errorRubro != null || errorPublico != null || errorTelefono != null) {
            _estado.update {
                it.copy(
                    errorNombre = errorNombre,
                    errorRubro = errorRubro,
                    errorPublico = errorPublico,
                    errorTelefono = errorTelefono,
                    error = null,
                    mensaje = null
                )
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null, mensaje = null) }
            val perfil = PerfilNegocio(
                nombreComercial = actual.nombreComercial.trim(),
                rubro = actual.rubro.trim(),
                publicoObjetivo = actual.publicoObjetivo.trim(),
                tono = actual.tono,
                descripcion = actual.descripcion.trim()
            )
            when (val resultado = perfiles.guardar(perfil)) {
                is Resultado.Exito -> {
                    // Si cambió el teléfono, se guarda también en la cuenta de la empresa.
                    val empresa = empresaActual
                    if (empresa != null && empresa.telefono != actual.telefono) {
                        val actualizada = empresa.copy(telefono = actual.telefono)
                        when (val cuenta = cuentas.guardar(actualizada)) {
                            is Resultado.Exito -> empresaActual = cuenta.dato
                            is Resultado.Error -> {
                                _estado.update { it.copy(guardando = false, error = cuenta.mensaje) }
                                return@launch
                            }
                        }
                    }
                    _estado.update {
                        it.copy(
                            guardando = false,
                            nombreComercial = resultado.dato.nombreComercial,
                            rubro = resultado.dato.rubro,
                            publicoObjetivo = resultado.dato.publicoObjetivo,
                            tono = resultado.dato.tono,
                            descripcion = resultado.dato.descripcion,
                            mensaje = "Perfil guardado."
                        )
                    }
                }

                is Resultado.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado.mensaje)
                }
            }
        }
    }

    companion object {
        private val TELEFONO_VALIDO = Regex("^(9\\d{8}|\\d{7})$")
    }
}
