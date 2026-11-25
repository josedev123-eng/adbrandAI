package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.AnuncioApi
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.RedSocial
import org.json.JSONObject

// HU 10 y 12: traduce el anuncio generado por Spring a los modelos de la app.
class AnuncioRepositoryRemoto : AnuncioRepository {

    override suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<AnuncioGenerado> =
        responder(AnuncioApi.generar(oferta, redSocial.valor))

    override suspend fun regenerar(id: Long): Resultado<AnuncioGenerado> =
        responder(AnuncioApi.regenerar(id))

    private fun responder(llamada: Result<RespuestaHttp>): Resultado<AnuncioGenerado> {
        val respuesta = llamada.getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return AnuncioJson.error(respuesta, "No se pudo generar el anuncio. Intenta de nuevo.")
        }
        return runCatching {
            Resultado.Exito(AnuncioJson.anuncio(JSONObject(respuesta.cuerpo)))
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }
}
