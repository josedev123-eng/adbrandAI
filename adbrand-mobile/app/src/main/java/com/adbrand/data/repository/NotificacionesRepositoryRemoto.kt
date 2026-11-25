package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.NotificacionesApi
import java.io.IOException
import org.json.JSONObject

// HU 50: traduce el registro de dispositivos de Spring a Resultado.
class NotificacionesRepositoryRemoto : NotificacionesRepository {

    override suspend fun registrar(token: String, plataforma: String, nombreEquipo: String): Resultado<Unit> =
        responder(NotificacionesApi.registrarDispositivo(token, plataforma, nombreEquipo))

    override suspend fun quitar(token: String): Resultado<Unit> =
        responder(NotificacionesApi.eliminarDispositivo(token))

    private fun responder(llamada: Result<com.adbrand.core.network.RespuestaHttp>): Resultado<Unit> {
        val respuesta = llamada.getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            val mensaje = runCatching { JSONObject(respuesta.cuerpo).optString("mensaje") }.getOrNull()
            return Resultado.Error(
                mensaje?.takeIf { it.isNotBlank() }
                    ?: "No se pudo sincronizar este teléfono con tus avisos.",
                respuesta.codigo
            )
        }
        return Resultado.Exito(Unit)
    }

    private fun errorDeRed(error: Throwable): Resultado.Error = Resultado.Error(
        if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
        else "Ocurrió un error inesperado."
    )
}
