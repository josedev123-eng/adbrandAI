package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 37: GET /api/reportes/campanas. Spring usa la empresa del usuario que inició sesión.
object InformeApi {

    suspend fun obtener(): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "reportes/campanas")
}
