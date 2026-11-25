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

    override suspend fun cancelar(id: Long): Resultado<Int> =
        accion(EstadosContenidoApi.cancelar(id), "No se pudo cancelar.") {
            runCatching { JSONObject(it).optInt("programacionesCanceladas", 0) }.getOrDefault(0)
        }

    override suspend fun reactivar(id: Long): Resultado<Unit> =
        accion(EstadosContenidoApi.reactivar(id), "No se pudo volver a activar.") { }

    override suspend fun eliminar(id: Long): Resultado<Unit> =
        accion(EstadosContenidoApi.eliminar(id), "No se pudo eliminar.") { }

    private fun <T> accion(
        llamada: Result<com.adbrand.core.network.RespuestaHttp>,
        porDefecto: String,
        leer: (String) -> T
    ): Resultado<T> {
        val respuesta = llamada.getOrElse {
            return Resultado.Error(
                if (it is IOException) "No se pudo conectar con el servidor. Revisa tu conexión." else porDefecto
            )
        }
        if (!respuesta.esExitosa) {
            val mensaje = runCatching { JSONObject(respuesta.cuerpo).optString("mensaje") }.getOrNull()
            return Resultado.Error(if (mensaje.isNullOrBlank()) porDefecto else mensaje, respuesta.codigo)
        }
        return Resultado.Exito(leer(respuesta.cuerpo))
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
                    motivo = texto(item, "motivo"),
                    titular = texto(item, "titular"),
                    texto = texto(item, "texto"),
                    fecha = texto(item, "fechaCreacion").take(10)
                )
            )
        }
    }

    private fun texto(json: JSONObject, clave: String): String = if (json.isNull(clave)) "" else json.optString(clave)
}
