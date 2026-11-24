package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.ReportesApi
import com.adbrand.domain.model.RangoFechas
import com.adbrand.domain.model.ReporteDescargado
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// HU 59: baja el PDF del informe desde Spring y lo guarda en el celular.
class ReportesRepositoryRemoto(
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ReportesRepository {

    override suspend fun descargarPdf(rango: RangoFechas, carpeta: File): Resultado<ReporteDescargado> {
        val respuesta = ReportesApi.informePdf(rango).getOrElse {
            // Criterio 2: sin conexión se avisa y no se crea nada.
            return Resultado.Error(
                if (it is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
                else "Ocurrió un error inesperado."
            )
        }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.texto), respuesta.codigo)
        }

        // El mismo nombre que usa la web: informe-campanas-AAAA-MM-DD.pdf
        val nombre = "informe-campanas-${hoy()}.pdf"
        return withContext(Dispatchers.IO) {
            runCatching { ReporteDescargado(nombre, GuardadoPdf.guardar(respuesta.bytes, carpeta, nombre)) }
                .fold(
                    onSuccess = { Resultado.Exito(it) },
                    onFailure = { Resultado.Error("No se pudo guardar el PDF en el celular.") }
                )
        }
    }

    private fun mensajeDeError(codigo: Int, cuerpo: String): String {
        val mensaje = runCatching { JSONObject(cuerpo).optString("mensaje") }.getOrNull()
        return when {
            !mensaje.isNullOrBlank() -> mensaje
            codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            else -> "No se pudo descargar el reporte. Intenta de nuevo."
        }
    }
}
