package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import com.adbrand.domain.model.CampoPreferencia
import org.json.JSONObject

// HU 51: preferencias de avisos de la empresa.
object PreferenciasApi {

    suspend fun obtener(): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "notificaciones/preferencias")

    suspend fun actualizar(campo: CampoPreferencia, valor: Boolean): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put(campo.clave, valor).toString()
        return ClienteApi.ejecutarAsync("PATCH", "notificaciones/preferencias", cuerpo)
    }
}
