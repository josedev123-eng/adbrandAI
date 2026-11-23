package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 57: GET /api/metricas/contenido.
object MetricasApi {

    suspend fun contenido(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "metricas/contenido")
}
