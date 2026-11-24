package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import org.json.JSONObject

// HU 53 y 54: GET /api/contenido/por-revisar, POST /api/contenido/{id}/aprobar y POST /api/contenido/{id}/rechazar.
object RevisionApi {

    suspend fun porRevisar(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "contenido/por-revisar")

    suspend fun aprobar(id: Long): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("POST", "contenido/$id/aprobar")

    // HU 54: el motivo es opcional; si viene vacío no se manda.
    suspend fun rechazar(id: Long, motivo: String?): Result<RespuestaHttp> {
        val cuerpo = if (motivo.isNullOrBlank()) null else JSONObject().put("motivo", motivo.trim()).toString()
        return ClienteApi.ejecutarAsync("POST", "contenido/$id/rechazar", cuerpo)
    }
}
