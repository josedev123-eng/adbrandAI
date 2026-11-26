package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.AnuncioOpcionesApi
import com.adbrand.domain.model.OpcionAnuncio
import com.adbrand.domain.model.OpcionElegida
import com.adbrand.domain.model.OpcionesAnuncio
import com.adbrand.domain.model.RedSocial
import com.adbrand.domain.model.VersionAnuncio
import com.adbrand.domain.model.VersionesAnuncio
import org.json.JSONArray
import org.json.JSONObject

// IA de imágenes y opciones (rama de Jose): lee las respuestas de Spring y las
// convierte en modelos de la app, conservando los códigos de error de negocio.
class AnuncioOpcionesRepositoryRemoto : AnuncioOpcionesRepository {

    override suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<OpcionesAnuncio> {
        val respuesta = AnuncioOpcionesApi.generar(oferta, redSocial.valor)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return AnuncioJson.error(respuesta, "No se pudieron generar las opciones. Intenta de nuevo.")
        }
        return runCatching { Resultado.Exito(opcionesDesdeJson(JSONObject(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    override suspend fun elegir(opcionId: Long): Resultado<OpcionElegida> {
        val respuesta = AnuncioOpcionesApi.elegir(opcionId)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return AnuncioJson.error(respuesta, "No se pudo guardar la opción elegida.")
        }
        return runCatching {
            val json = JSONObject(respuesta.cuerpo)
            Resultado.Exito(
                OpcionElegida(
                    anuncio = AnuncioJson.anuncio(json.getJSONObject("anuncio")),
                    opcion = opcionDesdeJson(json.getJSONObject("opcion"))
                )
            )
        }.getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    override suspend fun retocar(opcionId: Long, instruccion: String): Resultado<VersionesAnuncio> {
        val respuesta = AnuncioOpcionesApi.retocar(opcionId, instruccion)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        return responderVersiones(respuesta, "No se pudo retocar la imagen.")
    }

    override suspend fun versiones(opcionId: Long): Resultado<VersionesAnuncio> {
        val respuesta = AnuncioOpcionesApi.versiones(opcionId)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        return responderVersiones(respuesta, "No se pudo cargar el historial de versiones.")
    }

    override suspend fun usarVersion(opcionId: Long, numero: Int): Resultado<VersionesAnuncio> {
        val respuesta = AnuncioOpcionesApi.usarVersion(opcionId, numero)
            .getOrElse { return AnuncioJson.errorDeRed(it) }
        return responderVersiones(respuesta, "No se pudo cambiar de versión.")
    }

    private fun responderVersiones(
        respuesta: RespuestaHttp,
        mensajePorDefecto: String
    ): Resultado<VersionesAnuncio> {
        if (!respuesta.esExitosa) {
            return AnuncioJson.error(respuesta, mensajePorDefecto)
        }
        return runCatching { Resultado.Exito(versionesDesdeJson(JSONObject(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    internal fun opcionesDesdeJson(json: JSONObject): OpcionesAnuncio = OpcionesAnuncio(
        generacionId = json.optLong("generacionId").takeIf { it > 0 },
        oferta = AnuncioJson.texto(json, "oferta"),
        redSocial = RedSocial.desde(AnuncioJson.texto(json, "redSocial")),
        opciones = json.optJSONArray("opciones").aOpciones()
    )

    internal fun versionesDesdeJson(json: JSONObject): VersionesAnuncio = VersionesAnuncio(
        opcionId = json.optLong("opcionId"),
        versionActual = json.optInt("versionActual", 1),
        imagenActual = AnuncioJson.texto(json, "imagenActual"),
        retoquesRestantes = if (json.isNull("retoquesRestantes")) null else json.optInt("retoquesRestantes"),
        versiones = json.optJSONArray("versiones").aVersiones()
    )

    private fun opcionDesdeJson(json: JSONObject): OpcionAnuncio = OpcionAnuncio(
        id = json.optLong("id"),
        numero = json.optInt("numero"),
        estilo = AnuncioJson.texto(json, "estilo"),
        texto = AnuncioJson.texto(json, "texto"),
        imagen = AnuncioJson.texto(json, "imagen"),
        proveedorImagen = AnuncioJson.texto(json, "proveedorImagen"),
        imagenSimulada = json.optBoolean("imagenSimulada", false),
        textoSimulado = json.optBoolean("textoSimulado", false),
        elegida = json.optBoolean("elegida", false),
        diseno = AnuncioJson.texto(json, "diseno").ifBlank { null },
        titular = AnuncioJson.texto(json, "titular").ifBlank { null },
        angulo = AnuncioJson.texto(json, "angulo").ifBlank { null }
    )

    private fun JSONArray?.aOpciones(): List<OpcionAnuncio> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { indice ->
            optJSONObject(indice)?.let(::opcionDesdeJson)
        }
    }

    private fun JSONArray?.aVersiones(): List<VersionAnuncio> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { indice ->
            optJSONObject(indice)?.let { json ->
                VersionAnuncio(
                    numero = json.optInt("numero"),
                    instruccion = AnuncioJson.texto(json, "instruccion"),
                    imagen = AnuncioJson.texto(json, "imagen"),
                    proveedorImagen = AnuncioJson.texto(json, "proveedorImagen"),
                    imagenSimulada = json.optBoolean("imagenSimulada", false),
                    actual = json.optBoolean("actual", false)
                )
            }
        }
    }
}
