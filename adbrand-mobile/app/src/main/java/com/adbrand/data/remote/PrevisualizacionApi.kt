package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 45: GET /api/previsualizacion.
object PrevisualizacionApi {

    suspend fun obtener(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "previsualizacion")
}
