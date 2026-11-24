package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaArchivo
import com.adbrand.domain.model.RangoFechas

// HU 59: GET /api/reportes/campanas/pdf, el mismo PDF que descarga la web (HU 38 y 40), con el periodo elegido.
object ReportesApi {

    suspend fun informePdf(rango: RangoFechas): Result<RespuestaArchivo> =
        ClienteApi.descargarAsync("reportes/campanas/pdf" + MetricasApi.consulta(rango))
}
