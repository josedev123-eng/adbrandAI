package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.PrevisualizacionApi
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
import com.adbrand.domain.model.KitMarca
import com.adbrand.domain.model.LogoMarca
import com.adbrand.domain.model.Previsualizacion
import java.io.IOException
import org.json.JSONObject

// HU 45: traduce { kit, anuncios } del backend a los modelos de la app.
class PrevisualizacionRepositoryRemoto : PrevisualizacionRepository {

    override suspend fun obtener(formato: String): Resultado<Previsualizacion> {
        val respuesta = PrevisualizacionApi.obtener(formato).getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching { Resultado.Exito(desdeJson(JSONObject(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    override suspend fun imagen(contenidoId: Long): Resultado<String?> {
        val respuesta = PrevisualizacionApi.imagen(contenidoId).getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching { Resultado.Exito(texto(JSONObject(respuesta.cuerpo), "imagen").ifBlank { null }) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    override suspend fun registrarCompartido(contenidoId: Long, formato: String): Resultado<Unit> {
        val respuesta = PrevisualizacionApi.registrarCompartido(contenidoId, formato).getOrElse { return errorDeRed(it) }
        return if (respuesta.esExitosa) Resultado.Exito(Unit)
        else Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
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
        val formato = json.optJSONObject("formato")?.let { f ->
            FormatoVista(
                codigo = texto(f, "codigo").ifBlank { "CUADRADO" },
                nombre = texto(f, "nombre").ifBlank { "Cuadrado" },
                uso = texto(f, "uso"),
                ancho = f.optInt("ancho", 1080),
                alto = f.optInt("alto", 1080),
                maxCaracteres = f.optInt("maxCaracteres", 125)
            )
        } ?: FormatoVista()
        return Previsualizacion(kit = kit, anuncios = anuncios, formato = formato)
    }

    private fun kitDesde(json: JSONObject): KitMarca {
        val logo = json.optJSONObject("logoDiseno")?.let { diseno ->
            LogoMarca(
                texto = texto(diseno, "texto"),
                forma = texto(diseno, "forma").ifBlank { "REDONDEADO" },
                colorFondo = texto(diseno, "colorFondo").ifBlank { "#4F46E5" },
                colorTexto = texto(diseno, "colorTexto").ifBlank { "#FFFFFF" },
                tipografia = texto(diseno, "tipografia")
            )
        }
        return KitMarca(
            id = json.optLong("id").takeIf { it > 0 },
            nombreNegocio = texto(json, "nombreNegocio"),
            sector = texto(json, "sector"),
            logoConcepto = texto(json, "logoConcepto"),
            tipografiaPrimaria = texto(json, "tipografiaPrimaria"),
            tipografiaSecundaria = texto(json, "tipografiaSecundaria"),
            paletaColores = texto(json, "paletaColores"),
            vozMarca = texto(json, "vozMarca"),
            logoDiseno = logo
        )
    }

    private fun anuncioDesde(json: JSONObject): Anuncio = Anuncio(
        id = json.optLong("id").takeIf { it > 0 },
        texto = texto(json, "texto"),
        textoAjustado = texto(json, "textoAjustado").ifBlank { texto(json, "texto") },
        recortado = json.optBoolean("recortado", false),
        redSocial = texto(json, "redSocial").ifBlank { "INSTAGRAM" },
        tono = texto(json, "tono").ifBlank { "CERCANO" },
        estado = texto(json, "estado").ifBlank { "APROBADO" },
        fechaCreacion = texto(json, "fechaCreacion")
    )

    private fun texto(json: JSONObject, clave: String): String = if (json.isNull(clave)) "" else json.optString(clave)

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

    override suspend fun textoParaPublicar(contenidoId: Long): Resultado<String> {
        val respuesta = com.adbrand.core.network.ClienteApi.ejecutarAsync("GET", "contenido/anuncios/$contenidoId/publicar")
            .getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) return Resultado.Error("No se pudo preparar el texto para publicar.", respuesta.codigo)
        return runCatching { Resultado.Exito(JSONObject(respuesta.cuerpo).getString("textoParaPublicar")) }
            .getOrElse { Resultado.Error("La respuesta del servidor no tiene el formato esperado.") }
    }
}
