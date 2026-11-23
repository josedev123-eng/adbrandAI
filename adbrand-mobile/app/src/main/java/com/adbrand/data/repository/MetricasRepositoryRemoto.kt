package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.MetricasApi
import com.adbrand.domain.model.ContenidoPorMes
import com.adbrand.domain.model.MetricasContenido
import java.io.IOException
import org.json.JSONObject

// HU 57: traduce { total, aprobados, enRevision, rechazados, porMes } del backend al modelo de la app.
class MetricasRepositoryRemoto : MetricasRepository {

    override suspend fun contenido(): Resultado<MetricasContenido> {
        val respuesta = MetricasApi.contenido().getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching { Resultado.Exito(desdeJson(JSONObject(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    private fun desdeJson(json: JSONObject): MetricasContenido {
        val mesesJson = json.optJSONArray("porMes")
        val porMes = buildList {
            if (mesesJson != null) {
                for (indice in 0 until mesesJson.length()) {
                    val mes = mesesJson.getJSONObject(indice)
                    add(ContenidoPorMes(mes = mes.optString("mes"), cantidad = mes.optLong("cantidad")))
                }
            }
        }
        return MetricasContenido(
            total = json.optLong("total"),
            aprobados = json.optLong("aprobados"),
            enRevision = json.optLong("enRevision"),
            rechazados = json.optLong("rechazados"),
            porMes = porMes
        )
    }

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
            else -> "No se pudieron cargar las métricas. Intenta de nuevo."
        }
    }
}
