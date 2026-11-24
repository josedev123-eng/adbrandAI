package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.RevisionApi
import com.adbrand.domain.model.ContenidoRevision
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// HU 53: traduce la lista "por revisar" del backend y la respuesta de aprobar.
class RevisionRepositoryRemoto : RevisionRepository {

    override suspend fun porRevisar(): Resultado<List<ContenidoRevision>> {
        val respuesta = RevisionApi.porRevisar().getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta, "No se pudo cargar el contenido por revisar."), respuesta.codigo)
        }
        return runCatching { Resultado.Exito(listaDesde(JSONArray(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    override suspend fun aprobar(id: Long): Resultado<Unit> {
        val respuesta = RevisionApi.aprobar(id).getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta, "No se pudo aprobar el contenido."), respuesta.codigo)
        }
        return Resultado.Exito(Unit)
    }

    // HU 54: rechazar con un motivo opcional.
    override suspend fun rechazar(id: Long, motivo: String?): Resultado<Unit> {
        val respuesta = RevisionApi.rechazar(id, motivo).getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta, "No se pudo rechazar el contenido."), respuesta.codigo)
        }
        return Resultado.Exito(Unit)
    }

    private fun listaDesde(json: JSONArray): List<ContenidoRevision> = buildList {
        for (indice in 0 until json.length()) {
            val item = json.getJSONObject(indice)
            add(
                ContenidoRevision(
                    id = item.getLong("id"),
                    oferta = texto(item, "oferta"),
                    texto = texto(item, "texto"),
                    redSocial = texto(item, "redSocial").ifBlank { "INSTAGRAM" },
                    estado = texto(item, "estado").ifBlank { "APROBADO" },
                    motivoRevision = texto(item, "motivoRevision"),
                    puedeAprobar = item.optBoolean("puedeAprobar", false),
                    fechaCreacion = texto(item, "fechaCreacion")
                )
            )
        }
    }

    private fun texto(json: JSONObject, clave: String): String = if (json.isNull(clave)) "" else json.optString(clave)

    private fun errorDeRed(error: Throwable): Resultado.Error =
        Resultado.Error(
            if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
            else "Ocurrió un error inesperado."
        )

    private fun mensajeDeError(respuesta: RespuestaHttp, porDefecto: String): String {
        val mensaje = runCatching { JSONObject(respuesta.cuerpo).optString("mensaje") }.getOrNull()
        return when {
            !mensaje.isNullOrBlank() -> mensaje
            respuesta.codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            else -> porDefecto
        }
    }
}
