package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 53: GET /api/contenido/por-revisar y POST /api/contenido/{id}/aprobar.
object RevisionApi {

    suspend fun porRevisar(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "contenido/por-revisar")

    suspend fun aprobar(id: Long): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("POST", "contenido/$id/aprobar")
}
