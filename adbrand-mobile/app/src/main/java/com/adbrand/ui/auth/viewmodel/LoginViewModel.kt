package com.adbrand.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AuthRepository
import com.adbrand.data.repository.AuthRepositoryRemoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// HU 42: estado del formulario de inicio de sesión.
data class LoginEstado(
    val correo: String = "",
    val contrasena: String = "",
    val errorCorreo: String? = null,
    val errorContrasena: String? = null,
    val error: String? = null,
    val cargando: Boolean = false,
    val autenticado: Boolean = false
)

class LoginViewModel(
    private val repositorio: AuthRepository = AuthRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginEstado())
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    fun actualizarCorreo(valor: String) {
        _estado.update { it.copy(correo = valor, errorCorreo = null, error = null) }
    }

    fun actualizarContrasena(valor: String) {
        _estado.update { it.copy(contrasena = valor, errorContrasena = null, error = null) }
    }

    fun iniciarSesion() {
        val actual = _estado.value
        val errorCorreo = if (actual.correo.isBlank()) "Ingresa tu correo." else null
        val errorContrasena = if (actual.contrasena.isBlank()) "Ingresa tu contraseña." else null
        if (errorCorreo != null || errorContrasena != null) {
            _estado.update {
                it.copy(errorCorreo = errorCorreo, errorContrasena = errorContrasena, error = null)
            }
            return
        }
        if (actual.cargando) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = repositorio.iniciarSesion(actual.correo.trim(), actual.contrasena)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(cargando = false, autenticado = true, error = null)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }
}
