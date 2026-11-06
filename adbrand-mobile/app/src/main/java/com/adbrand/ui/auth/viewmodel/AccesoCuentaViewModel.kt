package com.adbrand.ui.auth.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AccesoCuentaRepository
import com.adbrand.data.repository.AccesoCuentaRepositoryRemoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccesoCuentaEstado(
    val nombre: String = "", val empresa: String = "", val correo: String = "",
    val contrasena: String = "", val repetir: String = "", val codigo: String = "",
    val nueva: String = "", val repetirNueva: String = "", val paso: Int = 1,
    val cargando: Boolean = false, val terminado: Boolean = false,
    val mensaje: String? = null, val error: String? = null
)

class AccesoCuentaViewModel(
    private val repo: AccesoCuentaRepository = AccesoCuentaRepositoryRemoto()
) : ViewModel() {
    private val _estado = MutableStateFlow(AccesoCuentaEstado())
    val estado: StateFlow<AccesoCuentaEstado> = _estado.asStateFlow()

    fun nombre(v: String) = editar { copy(nombre = v, error = null, mensaje = null) }
    fun empresa(v: String) = editar { copy(empresa = v, error = null, mensaje = null) }
    fun correo(v: String) = editar { copy(correo = v, error = null, mensaje = null) }
    fun contrasena(v: String) = editar { copy(contrasena = v, error = null, mensaje = null) }
    fun repetir(v: String) = editar { copy(repetir = v, error = null) }
    fun codigo(v: String) = editar { copy(codigo = v.filter(Char::isDigit).take(6), error = null) }
    fun nueva(v: String) = editar { copy(nueva = v, error = null) }
    fun repetirNueva(v: String) = editar { copy(repetirNueva = v, error = null) }

    fun registrar() {
        val s = _estado.value
        val problema = when {
            s.nombre.isBlank() -> "Escribe tu nombre."
            s.empresa.isBlank() -> "Escribe el nombre de tu empresa."
            !correoValido(s.correo) -> "Escribe un correo válido."
            !claveValida(s.contrasena) -> "Usa 8 caracteres o más con mayúscula, minúscula y número, sin espacios."
            s.contrasena != s.repetir -> "Las contraseñas no coinciden."
            else -> null
        }
        if (problema != null) return editar { copy(error = problema, mensaje = null) }
        enviar({ repo.registrar(s.nombre.trim(), s.empresa.trim(), s.correo.trim(), s.contrasena) }, terminar = true)
    }

    fun pedirCodigo() {
        val s = _estado.value
        if (!correoValido(s.correo)) return editar { copy(error = "Escribe un correo válido.") }
        enviar({ repo.solicitarCodigo(s.correo.trim()) }) { editar { copy(paso = 2) } }
    }

    fun cambiarContrasena() {
        val s = _estado.value
        val problema = when {
            !correoValido(s.correo) -> "Escribe un correo válido."
            s.codigo.length != 6 -> "Ingresa el código de seis dígitos."
            !claveValida(s.nueva) -> "Usa 8 caracteres o más con mayúscula, minúscula y número, sin espacios."
            s.nueva != s.repetirNueva -> "Las contraseñas no coinciden."
            else -> null
        }
        if (problema != null) return editar { copy(error = problema, mensaje = null) }
        enviar({ repo.cambiarContrasena(s.correo.trim(), s.codigo, s.nueva) }, terminar = true)
    }

    fun otroCodigo() = editar { copy(paso = 1, codigo = "", nueva = "", repetirNueva = "", mensaje = null, error = null) }

    private fun editar(bloque: AccesoCuentaEstado.() -> AccesoCuentaEstado) { _estado.update(bloque) }

    private fun enviar(accion: suspend () -> Resultado<String>, terminar: Boolean = false, despues: () -> Unit = {}) {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            editar { copy(cargando = true, error = null, mensaje = null) }
            when (val r = accion()) {
                is Resultado.Exito -> {
                    editar { copy(cargando = false, mensaje = r.dato, terminado = terminado || terminar) }
                    despues()
                }
                is Resultado.Error -> editar { copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    private fun correoValido(v: String) = Patterns.EMAIL_ADDRESS.matcher(v.trim()).matches()
    private fun claveValida(v: String) = v.length in 8..72 && v.any(Char::isUpperCase) && v.any(Char::isLowerCase) && v.any(Char::isDigit) && v.none(Char::isWhitespace)
}
