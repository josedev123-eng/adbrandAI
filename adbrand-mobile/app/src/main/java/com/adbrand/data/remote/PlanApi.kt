package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 52: plan actual de la empresa y catálogo público de planes.
object PlanApi {

    suspend fun mia(): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "suscripciones/mia")

    suspend fun planes(): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "planes")
}
