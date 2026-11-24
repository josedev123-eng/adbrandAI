package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.MetricasApi
import com.adbrand.domain.model.ContenidoPorMes
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.ProximaPublicacion
import com.adbrand.domain.model.PublicacionesPorRed
import com.adbrand.domain.model.RangoFechas
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// HU 57: traduce { total, aprobados, enRevision, rechazados, porMes } del backend al modelo de la app.
// HU 58: traduce { programadas, publicadas, canceladas, porRed, proximas }.
class MetricasRepositoryRemoto : MetricasRepository {

    override suspend fun publicaciones(rango: RangoFechas): Resultado<MetricasPublicaciones> {
        val respuesta = MetricasApi.publicaciones(rango).getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching { Resultado.Exito(publicacionesDesdeJson(JSONObject(respuesta.cuerpo))) }
            .getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    private fun publicacionesDesdeJson(json: JSONObject): MetricasPublicaciones = MetricasPublicaciones(
        programadas = json.optLong("programadas"),
        publicadas = json.optLong("publicadas"),
        canceladas = json.optLong("canceladas"),
        porRed = lista(json.optJSONArray("porRed")) { red ->
            PublicacionesPorRed(red.optString("redSocial"), red.optLong("programadas"), red.optLong("publicadas"))
        },
        proximas = lista(json.optJSONArray("proximas")) { p ->
            ProximaPublicacion(
                id = p.optLong("id"),
                titulo = p.optString("titulo"),
                redSocial = p.optString("redSocial"),
                fecha = p.optString("fecha"),
                hora = p.optString("hora"),
                campanaNombre = if (p.isNull("campanaNombre")) null else p.optString("campanaNombre"),
                campanaColor = if (p.isNull("campanaColor")) null else p.optString("campanaColor")
            )
        }
    )

    private fun <T> lista(arreglo: JSONArray?, convertir: (JSONObject) -> T): List<T> = buildList {
        if (arreglo != null) {
            for (indice in 0 until arreglo.length()) add(convertir(arreglo.getJSONObject(indice)))
        }
    }

    override suspend fun contenido(rango: RangoFechas): Resultado<MetricasContenido> {
        val respuesta = MetricasApi.contenido(rango).getOrElse { return errorDeRed(it) }
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
