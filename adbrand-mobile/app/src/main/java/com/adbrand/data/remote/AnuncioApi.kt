package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import org.json.JSONObject

// HU 10 y 12: POST /api/contenido/anuncios y /{id}/regenerar.
object AnuncioApi {

    suspend fun generar(oferta: String, redSocial: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("oferta", oferta)
            .put("redSocial", redSocial)
            .toString()
        return ClienteApi.ejecutarAsync("POST", "contenido/anuncios", cuerpo)
    }

    suspend fun regenerar(id: Long): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("POST", "contenido/anuncios/$id/regenerar", "{}")
}
