package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 45: GET /api/previsualizacion.
object PrevisualizacionApi {

    fun obtener(): Result<RespuestaHttp> = ClienteApi.ejecutar("GET", "previsualizacion")
}
