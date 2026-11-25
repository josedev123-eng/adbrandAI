package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.CampanaApi
import com.adbrand.domain.model.Campana
import java.io.IOException
import org.json.JSONObject

// HU 49: traduce la ficha de campaña que devuelve Spring a los modelos de la app.
class CampanaRepositoryRemoto : CampanaRepository {

    override suspend fun obtener(id: Long): Resultado<Campana> {
        val respuesta = CampanaApi.obtener(id).getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return errorDelServidor(respuesta)
        }
        return runCatching {
            Resultado.Exito(campanaDesde(JSONObject(respuesta.cuerpo)))
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    internal fun campanaDesde(json: JSONObject): Campana = Campana(
        id = json.optLong("id"),
        nombre = texto(json, "nombre"),
        color = texto(json, "color").ifBlank { "#1F3D33" },
        tipo = texto(json, "tipo").ifBlank { "CAMPANA" },
        fechaInicio = texto(json, "fechaInicio"),
        fechaFin = texto(json, "fechaFin")
    )

    private fun errorDelServidor(respuesta: RespuestaHttp): Resultado.Error {
        val json = runCatching { JSONObject(respuesta.cuerpo) }.getOrNull()
        val codigoError = json?.optString("codigo")?.takeIf { it.isNotBlank() }
        val mensaje = json?.optString("mensaje")?.takeIf { !it.isNullOrBlank() }
        val final = when {
            mensaje != null -> mensaje
            respuesta.codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            respuesta.codigo == 404 -> "Esta campaña ya no existe o no es de tu empresa."
            else -> "No se pudo cargar la campaña. Intenta de nuevo."
        }
        return Resultado.Error(final, respuesta.codigo, codigoError)
    }

    private fun errorDeRed(error: Throwable): Resultado.Error = Resultado.Error(
        if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
        else "Ocurrió un error inesperado."
    )

    private fun texto(json: JSONObject, clave: String): String =
        if (json.isNull(clave)) "" else json.optString(clave)
}
