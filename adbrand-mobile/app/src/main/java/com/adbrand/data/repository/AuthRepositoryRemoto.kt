package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.core.session.AlmacenSesion
import com.adbrand.core.session.GestorSesion
import com.adbrand.data.remote.AuthApi
import com.adbrand.domain.model.Usuario
import java.io.IOException
import org.json.JSONObject

// HU 42: traduce la respuesta de Spring a un Usuario y guarda los dos tokens.
class AuthRepositoryRemoto(
    private val sesion: AlmacenSesion = GestorSesion
) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario> {
        val respuesta = AuthApi.iniciarSesion(correo.trim(), contrasena)
            .getOrElse { return sinConexion(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return try {
            val json = JSONObject(respuesta.cuerpo)
            val usuarioJson = json.getJSONObject("usuario")
            val token = json.getString("token")
            val renovacion = json.optString("tokenRenovacion").takeIf { it.isNotBlank() }
            sesion.guardarSesion(token, renovacion)
            Resultado.Exito(
                Usuario(
                    id = usuarioJson.getLong("id"),
                    nombre = usuarioJson.optString("nombre"),
                    nombreEmpresa = usuarioJson.optString("nombreEmpresa"),
                    correo = usuarioJson.optString("correo")
                )
            )
        } catch (error: Exception) {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    override suspend fun cerrarSesion(): Resultado<Unit> {
        val renovacion = sesion.tokenRenovacion()
        if (!renovacion.isNullOrBlank()) {
            AuthApi.cerrarSesion(renovacion)
        }
        sesion.limpiar()
        return Resultado.Exito(Unit)
    }

    private fun sinConexion(error: Throwable): Resultado.Error =
        Resultado.Error(
            if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
            else "Ocurrió un error inesperado."
        )

    private fun mensajeDeError(codigo: Int, cuerpo: String): String {
        val mensaje = runCatching { JSONObject(cuerpo).optString("mensaje") }.getOrNull()
        return when {
            !mensaje.isNullOrBlank() -> mensaje
            codigo == 401 -> "Correo o contraseña incorrectos."
            else -> "No se pudo iniciar sesión. Intenta de nuevo."
        }
    }
}
