package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 49: GET /api/campanas/{id}. Spring solo devuelve la campaña si es de la
// empresa que inició sesión; si no, responde 404 CAMPANA_NO_ENCONTRADA.
object CampanaApi {

    suspend fun obtener(id: Long): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "campanas/$id")
}
