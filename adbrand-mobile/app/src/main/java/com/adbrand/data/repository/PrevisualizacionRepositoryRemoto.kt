package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.PrevisualizacionApi
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.KitMarca
import com.adbrand.domain.model.LogoMarca
import com.adbrand.domain.model.Previsualizacion
import java.io.IOException
import org.json.JSONObject

// HU 45: traduce { kit, anuncios } del backend a los modelos de la app.
class PrevisualizacionRepositoryRemoto : PrevisualizacionRepository {

    override suspend fun obtener(): Resultado<Previsualizacion> {
        val respuesta = PrevisualizacionApi.obtener().getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching { Resultado.Exito(desdeJson(JSONObject(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    private fun desdeJson(json: JSONObject): Previsualizacion {
        val kit = json.optJSONObject("kit")?.let(::kitDesde)
        val anunciosJson = json.optJSONArray("anuncios")
        val anuncios = buildList {
            if (anunciosJson != null) {
                for (indice in 0 until anunciosJson.length()) {
                    add(anuncioDesde(anunciosJson.getJSONObject(indice)))
                }
            }
        }
        return Previsualizacion(kit = kit, anuncios = anuncios)
    }

    private fun kitDesde(json: JSONObject): KitMarca {
        val logo = json.optJSONObject("logoDiseno")?.let { diseno ->
            LogoMarca(
                texto = diseno.optString("texto"),
                forma = diseno.optString("forma", "REDONDEADO"),
                colorFondo = diseno.optString("colorFondo", "#4F46E5"),
                colorTexto = diseno.optString("colorTexto", "#FFFFFF"),
                tipografia = diseno.optString("tipografia")
            )
        }
        return KitMarca(
            id = json.optLong("id").takeIf { it > 0 },
            nombreNegocio = json.optString("nombreNegocio"),
            sector = json.optString("sector"),
            logoConcepto = json.optString("logoConcepto"),
            tipografiaPrimaria = json.optString("tipografiaPrimaria"),
            tipografiaSecundaria = json.optString("tipografiaSecundaria"),
            paletaColores = json.optString("paletaColores"),
            vozMarca = json.optString("vozMarca"),
            logoDiseno = logo
        )
    }

    private fun anuncioDesde(json: JSONObject): Anuncio = Anuncio(
        id = json.optLong("id").takeIf { it > 0 },
        texto = json.optString("texto"),
        redSocial = json.optString("redSocial", "INSTAGRAM"),
        tono = json.optString("tono", "CERCANO"),
        estado = json.optString("estado", "APROBADO"),
        fechaCreacion = json.optString("fechaCreacion")
    )

    private fun errorDeRed(error: Throwable): Resultado.Error =
        Resultado.Error(
            if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
            else "Ocurrió un error inesperado."
        )

    private fun mensajeDeError(codigo: Int, cuerpo: String): String {
        val mensaje = runCatching { JSONObject(cuerpo).optString("mensaje") }.getOrNull()
        return when {
            !mensaje.isNullOrBlank() -> mensaje
            codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            else -> "No se pudo cargar la previsualización. Intenta de nuevo."
        }
    }
}
