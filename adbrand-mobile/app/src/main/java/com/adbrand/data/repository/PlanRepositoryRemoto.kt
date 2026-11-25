package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.PlanApi
import com.adbrand.domain.model.MiSuscripcion
import com.adbrand.domain.model.PlanDisponible
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// HU 52: lee el plan actual y el catálogo de planes que devuelve Spring.
class PlanRepositoryRemoto : PlanRepository {

    override suspend fun mia(): Resultado<MiSuscripcion> {
        val respuesta = PlanApi.mia().getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return errorDelServidor(respuesta, "No se pudo cargar tu plan.")
        }
        return runCatching {
            Resultado.Exito(miSuscripcionDesde(JSONObject(respuesta.cuerpo)))
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    override suspend fun planes(): Resultado<List<PlanDisponible>> {
        val respuesta = PlanApi.planes().getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return errorDelServidor(respuesta, "No se pudieron cargar los planes.")
        }
        return runCatching {
            val lista = JSONArray(respuesta.cuerpo)
            Resultado.Exito((0 until lista.length()).mapNotNull { indice ->
                lista.optJSONObject(indice)?.let(::planDesde)
            })
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    internal fun miSuscripcionDesde(json: JSONObject): MiSuscripcion = MiSuscripcion(
        plan = texto(json, "plan"),
        estado = texto(json, "estado").ifBlank { "SIN_PLAN" },
        fechaVencimiento = texto(json, "fechaVencimiento"),
        renovacionAutomatica = json.optBoolean("renovacionAutomatica", false),
        diasRestantes = json.optLong("diasRestantes", 0),
        monto = json.optDouble("monto", 0.0)
    )

    internal fun planDesde(json: JSONObject): PlanDisponible = PlanDisponible(
        id = json.optLong("id"),
        nombre = texto(json, "nombre"),
        precio = json.optDouble("precio", 0.0),
        limiteTokens = json.optInt("limiteTokens", 0),
        descripcion = texto(json, "descripcion")
    )

    private fun errorDelServidor(respuesta: RespuestaHttp, porDefecto: String): Resultado.Error {
        val json = runCatching { JSONObject(respuesta.cuerpo) }.getOrNull()
        val mensaje = json?.optString("mensaje")?.takeIf { !it.isNullOrBlank() }
        val final = when {
            mensaje != null -> mensaje
            respuesta.codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            else -> porDefecto
        }
        return Resultado.Error(final, respuesta.codigo)
    }

    private fun errorDeRed(error: Throwable): Resultado.Error = Resultado.Error(
        if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
        else "Ocurrió un error inesperado."
    )

    private fun texto(json: JSONObject, clave: String): String =
        if (json.isNull(clave)) "" else json.optString(clave)
}
