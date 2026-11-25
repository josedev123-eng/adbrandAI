package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.EstadosContenidoApi
import com.adbrand.domain.model.ContenidoConEstado
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// HU 56: traduce la lista de estados que devuelve Spring.
class EstadosContenidoRepositoryRemoto : EstadosContenidoRepository {

    override suspend fun estados(): Resultado<List<ContenidoConEstado>> {
        val respuesta = EstadosContenidoApi.estados().getOrElse {
            return Resultado.Error(
                if (it is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
                else "Ocurrió un error inesperado."
            )
        }
        if (!respuesta.esExitosa) {
            val mensaje = runCatching { JSONObject(respuesta.cuerpo).optString("mensaje") }.getOrNull()
            return Resultado.Error(
                when {
                    !mensaje.isNullOrBlank() -> mensaje
                    respuesta.codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
                    else -> "No se pudo cargar el estado de tus contenidos."
                },
                respuesta.codigo
            )
        }
        return runCatching { Resultado.Exito(listaDesde(JSONArray(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    private fun listaDesde(json: JSONArray): List<ContenidoConEstado> = buildList {
        for (indice in 0 until json.length()) {
            val item = json.getJSONObject(indice)
            add(
                ContenidoConEstado(
                    id = item.getLong("id"),
                    oferta = texto(item, "oferta"),
                    redSocial = texto(item, "redSocial").ifBlank { "INSTAGRAM" },
                    estadoActual = texto(item, "estadoActual").ifBlank { "PENDIENTE" },
                    motivo = texto(item, "motivo")
                )
            )
        }
    }

    private fun texto(json: JSONObject, clave: String): String = if (json.isNull(clave)) "" else json.optString(clave)
}
