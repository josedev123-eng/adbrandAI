package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import com.adbrand.domain.model.RangoFechas

// HU 57: GET /api/metricas/contenido. HU 58: GET /api/metricas/publicaciones.
// HU 60: los dos aceptan ?desde=AAAA-MM-DD&hasta=AAAA-MM-DD; las fechas vacías no se mandan.
object MetricasApi {

    suspend fun contenido(rango: RangoFechas): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "metricas/contenido" + consulta(rango))

    suspend fun publicaciones(rango: RangoFechas): Result<RespuestaHttp> =
        ClienteApi.ejecutarAsync("GET", "metricas/publicaciones" + consulta(rango))

    internal fun consulta(rango: RangoFechas): String {
        val partes = listOfNotNull(
            rango.desde?.let { "desde=$it" },
            rango.hasta?.let { "hasta=$it" }
        )
        return if (partes.isEmpty()) "" else "?" + partes.joinToString("&")
    }
}
