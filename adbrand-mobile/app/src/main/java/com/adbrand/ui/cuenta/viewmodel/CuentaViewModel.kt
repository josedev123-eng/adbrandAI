package com.adbrand.ui.cuenta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AuthRepository
import com.adbrand.data.repository.AuthRepositoryFirebase
import com.adbrand.data.repository.CuentaRepository
import com.adbrand.data.repository.CuentaRepositoryRemoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// nombre y correo: para la cabecera de "Cuenta". sesionCerrada: la pantalla vuelve al inicio de sesión.
data class CuentaEstado(
    val nombre: String = "",
    val correo: String = "",
    val cerrando: Boolean = false,
    val sesionCerrada: Boolean = false
)

// ViewModel de la pestaña "Cuenta": muestra de quién es la cuenta y cierra la sesión.
class CuentaViewModel(
    private val auth: AuthRepository = AuthRepositoryFirebase(),
    private val cuenta: CuentaRepository = CuentaRepositoryRemoto()
) : ViewModel() {

    private val _estado = MutableStateFlow(CuentaEstado())
    val estado: StateFlow<CuentaEstado> = _estado.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            val resultado = cuenta.obtener()
            if (resultado is Resultado.Exito) {
                _estado.update {
                    it.copy(nombre = resultado.dato.nombreEmpresa, correo = resultado.dato.correo)
                }
            }
        }
    }

    fun cerrarSesion() {
        if (_estado.value.cerrando) return
        viewModelScope.launch {
            _estado.update { it.copy(cerrando = true) }
            auth.cerrarSesion()
            _estado.update { it.copy(cerrando = false, sesionCerrada = true) }
        }
    }
}
