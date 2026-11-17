package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp

// HU 45: GET /api/previsualizacion.
object PrevisualizacionApi {

    // HU 46: formato = CUADRADO, VERTICAL u HORIZONTAL.
    suspend fun obtener(formato: String = "CUADRADO"): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "previsualizacion?formato=$formato")

    // HU 46: la imagen de un anuncio, solo cuando se abre su vista previa como publicación (pesa).
    suspend fun imagen(contenidoId: Long): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "previsualizacion/anuncios/$contenidoId/imagen")
}
