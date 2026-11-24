package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.RangoFechas
import com.adbrand.domain.model.ReporteDescargado
import java.io.File

// HU 59: contrato para probar la descarga del reporte sin red.
interface ReportesRepository {

    // Descarga el PDF del periodo y lo guarda en la carpeta. Si falla, no se crea ningún archivo.
    suspend fun descargarPdf(rango: RangoFechas, carpeta: File): Resultado<ReporteDescargado>
}
