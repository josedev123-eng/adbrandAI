package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import org.json.JSONObject

// HU 50: el teléfono se registra al iniciar sesión y se da de baja al cerrarla.
object NotificacionesApi {

    suspend fun registrarDispositivo(token: String, plataforma: String, nombreEquipo: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("fcmToken", token)
            .put("platform", plataforma)
            .put("deviceName", nombreEquipo)
            .toString()
        return ClienteApi.ejecutarAsync("POST", "notificaciones/dispositivos", cuerpo)
    }

    suspend fun eliminarDispositivo(token: String): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("DELETE", "notificaciones/dispositivos/$token")
}
