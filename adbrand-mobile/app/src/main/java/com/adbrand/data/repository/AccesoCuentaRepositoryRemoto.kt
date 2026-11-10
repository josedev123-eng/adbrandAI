package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.AuthApi
import java.io.IOException
import org.json.JSONObject

class AccesoCuentaRepositoryRemoto : AccesoCuentaRepository {
    override suspend fun registrar(nombre: String, empresa: String, correo: String, contrasena: String) =
        convertir(AuthApi.registrar(nombre, empresa, correo, contrasena), "Cuenta creada. Ya puedes iniciar sesión.")

    override suspend fun solicitarCodigo(correo: String) =
        convertir(AuthApi.solicitarCodigo(correo), "Si el correo está registrado, te enviamos un código.")

    override suspend fun cambiarContrasena(correo: String, codigo: String, contrasena: String) =
        convertir(AuthApi.cambiarContrasena(correo, codigo, contrasena), "Tu contraseña se cambió. Ya puedes iniciar sesión.")

    private fun convertir(llamada: Result<RespuestaHttp>, mensajeCorrecto: String): Resultado<String> {
        val respuesta = llamada.getOrElse {
            return Resultado.Error(if (it is IOException) "No se pudo conectar. Revisa tu conexión." else "Ocurrió un error inesperado.")
        }
        if (respuesta.esExitosa) return Resultado.Exito(leerMensaje(respuesta.cuerpo).ifBlank { mensajeCorrecto })
        val mensaje = leerMensaje(respuesta.cuerpo).ifBlank {
            when (respuesta.codigo) {
                400 -> "Revisa los datos ingresados e inténtalo otra vez."
                409 -> "Ese correo ya tiene una cuenta."
                else -> "No se pudo completar la solicitud."
            }
        }
        return Resultado.Error(mensaje, respuesta.codigo)
    }

    private fun leerMensaje(cuerpo: String) = runCatching {
        JSONObject(cuerpo).optString("mensaje").takeUnless { it == "null" }.orEmpty()
    }.getOrDefault("")
}
