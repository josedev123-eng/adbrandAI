package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 56: GET /api/contenido/estados, el estado actual de cada contenido de la empresa.
object EstadosContenidoApi {

    suspend fun estados(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "contenido/estados")
}
