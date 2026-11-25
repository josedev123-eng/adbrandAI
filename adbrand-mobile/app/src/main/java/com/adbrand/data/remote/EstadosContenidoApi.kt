package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 56: GET /api/contenido/estados, el estado actual de cada contenido de la empresa.
object EstadosContenidoApi {

    suspend fun estados(): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("GET", "contenido/estados")

    // HU 63: cancelar en AdBrand, volver a activar y eliminar (solo cancelados).
    suspend fun cancelar(id: Long): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("POST", "contenido/$id/cancelar-publicacion", "{}")
    suspend fun reactivar(id: Long): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("POST", "contenido/$id/reactivar", "{}")
    suspend fun eliminar(id: Long): Result<RespuestaHttp> = ClienteApi.ejecutarAsync("DELETE", "contenido/$id")
}
