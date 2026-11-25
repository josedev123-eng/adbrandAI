package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import org.json.JSONObject

// IA de imágenes y opciones (rama de Jose): "Generar → Elegir → Retocar".
// Las 4 opciones traen texto e imagen; elegir guarda el anuncio y retocar crea
// una versión nueva de la imagen.
object AnuncioOpcionesApi {

    private const val BASE = "contenido/anuncios/opciones"

    suspend fun generar(oferta: String, redSocial: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("oferta", oferta)
            .put("redSocial", redSocial)
            .toString()
        return ClienteApi.ejecutarAsync("POST", BASE, cuerpo)
    }

    suspend fun elegir(opcionId: Long): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("POST", "$BASE/$opcionId/elegir", "{}")

    suspend fun retocar(opcionId: Long, instruccion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("instruccion", instruccion).toString()
        return ClienteApi.ejecutarAsync("POST", "$BASE/$opcionId/retocar", cuerpo)
    }

    suspend fun versiones(opcionId: Long): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "$BASE/$opcionId/versiones")

    suspend fun usarVersion(opcionId: Long, numero: Int): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("POST", "$BASE/$opcionId/versiones/$numero/usar", "{}")
}
