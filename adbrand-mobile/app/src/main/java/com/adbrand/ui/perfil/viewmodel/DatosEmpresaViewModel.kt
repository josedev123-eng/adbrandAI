package com.adbrand.ui.perfil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AuthRepository
import com.adbrand.data.repository.AuthRepositoryRemoto
import com.adbrand.data.repository.CuentaRepository
import com.adbrand.data.repository.CuentaRepositoryRemoto
import com.adbrand.domain.model.Empresa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 41: estado de la pantalla "Datos de la empresa".
data class DatosEmpresaEstado(
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val nombreEmpresa: String = "",
    val correo: String = "",
    val ruc: String = "",
    val razonSocial: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val errorRuc: String? = null,
    val errorRazonSocial: String? = null,
    val errorTelefono: String? = null,
    val errorDireccion: String? = null,
    val error: String? = null,
    val mensaje: String? = null,
    val sesionCerrada: Boolean = false
)

class DatosEmpresaViewModel(
    private val cuenta: CuentaRepository = CuentaRepositoryRemoto(),
    private val auth: AuthRepository = AuthRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(DatosEmpresaEstado())
    val estado: StateFlow<DatosEmpresaEstado> = _estado.asStateFlow()

    fun cargar() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, mensaje = null) }
            when (val resultado = cuenta.obtener()) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        cargando = false,
                        nombreEmpresa = resultado.dato.nombreEmpresa,
                        correo = resultado.dato.correo,
                        ruc = resultado.dato.ruc,
                        razonSocial = resultado.dato.razonSocial,
                        telefono = resultado.dato.telefono,
                        direccion = resultado.dato.direccion
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun actualizarRuc(valor: String) {
        _estado.update { it.copy(ruc = valor.filter(Char::isDigit).take(11), errorRuc = null, mensaje = null) }
    }

    fun actualizarRazonSocial(valor: String) {
        _estado.update { it.copy(razonSocial = valor, errorRazonSocial = null, mensaje = null) }
    }

    fun actualizarTelefono(valor: String) {
        _estado.update { it.copy(telefono = valor.filter(Char::isDigit).take(9), errorTelefono = null, mensaje = null) }
    }

    fun actualizarDireccion(valor: String) {
        _estado.update { it.copy(direccion = valor, errorDireccion = null, mensaje = null) }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando) return

        val errorRuc = if (!RUC_VALIDO.matches(actual.ruc)) {
            "El RUC tiene 11 dígitos y empieza en 10 o 20."
        } else null
        val errorRazonSocial = if (actual.razonSocial.isBlank()) "Ingresa la razón social." else null
        val errorTelefono = if (actual.telefono.isNotBlank() && !TELEFONO_VALIDO.matches(actual.telefono)) {
            "Escribe un celular de 9 dígitos que empiece en 9, o un fijo de 7 dígitos."
        } else null
        val errorDireccion = if (actual.direccion.isBlank()) "Ingresa la dirección." else null

        if (errorRuc != null || errorRazonSocial != null || errorTelefono != null || errorDireccion != null) {
            _estado.update {
                it.copy(
                    errorRuc = errorRuc,
                    errorRazonSocial = errorRazonSocial,
                    errorTelefono = errorTelefono,
                    errorDireccion = errorDireccion,
                    error = null,
                    mensaje = null
                )
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, error = null, mensaje = null) }
            val empresa = Empresa(
                nombreEmpresa = actual.nombreEmpresa,
                correo = actual.correo,
                ruc = actual.ruc,
                razonSocial = actual.razonSocial.trim(),
                telefono = actual.telefono.trim(),
                direccion = actual.direccion.trim()
            )
            when (val resultado = cuenta.guardar(empresa)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(
                        guardando = false,
                        ruc = resultado.dato.ruc,
                        razonSocial = resultado.dato.razonSocial,
                        telefono = resultado.dato.telefono,
                        direccion = resultado.dato.direccion,
                        mensaje = "Datos guardados."
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(guardando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            auth.cerrarSesion()
            _estado.update { it.copy(sesionCerrada = true) }
        }
    }

    companion object {
        private val RUC_VALIDO = Regex("^(10|20)\\d{9}$")
        private val TELEFONO_VALIDO = Regex("^(9\\d{8}|\\d{7})$")
    }
}
