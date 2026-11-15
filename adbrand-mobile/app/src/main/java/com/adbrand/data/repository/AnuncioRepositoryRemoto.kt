package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.AnuncioApi
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.RedSocial
import com.adbrand.domain.model.Tono
import java.io.IOException
import org.json.JSONObject

// HU 10 y 12: traduce el anuncio generado por Spring a los modelos de la app.
class AnuncioRepositoryRemoto : AnuncioRepository {

    override suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<AnuncioGenerado> =
        responder(AnuncioApi.generar(oferta, redSocial.valor))

    override suspend fun regenerar(id: Long): Resultado<AnuncioGenerado> =
        responder(AnuncioApi.regenerar(id))

    private fun responder(llamada: Result<RespuestaHttp>): Resultado<AnuncioGenerado> {
        val respuesta = llamada.getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return errorDelServidor(respuesta)
        }
        return runCatching {
            Resultado.Exito(desdeJson(JSONObject(respuesta.cuerpo)))
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    private fun desdeJson(json: JSONObject): AnuncioGenerado = AnuncioGenerado(
        id = json.optLong("id").takeIf { it > 0 },
        texto = texto(json, "texto"),
        redSocial = RedSocial.desde(texto(json, "redSocial")),
        tono = tonoDesde(texto(json, "tono")),
        simulado = json.optBoolean("simulado", false),
        estado = textoConDefecto(json, "estado", "APROBADO"),
        motivoRevision = texto(json, "motivoRevision")
    )

    // Spring devuelve { codigo, mensaje, campos }. Se conservan para que la pantalla
    // distinga falta de perfil, IA caída y errores de validación por campo.
    private fun errorDelServidor(respuesta: RespuestaHttp): Resultado.Error {
        val json = runCatching { JSONObject(respuesta.cuerpo) }.getOrNull()
        val codigoError = json?.optString("codigo")?.takeIf { it.isNotBlank() }
        val mensaje = json?.optString("mensaje")?.takeIf { !it.isNullOrBlank() }
        val campos = buildMap {
            val objetos = json?.optJSONObject("campos") ?: return@buildMap
            objetos.keys().forEach { clave -> put(clave, objetos.optString(clave)) }
        }
        val mensajeFinal = when {
            mensaje != null -> mensaje
            respuesta.codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            respuesta.codigo == 404 -> "Todavía no registraste el perfil de tu negocio."
            respuesta.codigo >= 500 -> "El servicio de IA no responde en este momento. Intenta de nuevo en unos minutos."
            else -> "No se pudo generar el anuncio. Intenta de nuevo."
        }
        return Resultado.Error(mensajeFinal, respuesta.codigo, codigoError, campos)
    }

    private fun tonoDesde(valor: String): Tono =
        Tono.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) } ?: Tono.CERCANO

    private fun texto(json: JSONObject, clave: String): String =
        if (json.isNull(clave)) "" else json.optString(clave)

    private fun textoConDefecto(json: JSONObject, clave: String, defecto: String): String =
        texto(json, clave).ifBlank { defecto }

    private fun errorDeRed(error: Throwable): Resultado.Error =
        Resultado.Error(
            if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
            else "Ocurrió un error inesperado."
        )
}
