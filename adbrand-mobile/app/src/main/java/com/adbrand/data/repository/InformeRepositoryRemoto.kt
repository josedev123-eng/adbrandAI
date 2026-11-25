package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.InformeApi
import com.adbrand.domain.model.CampanaInforme
import com.adbrand.domain.model.InformeCampanas
import com.adbrand.domain.model.RedSocial
import java.io.IOException
import org.json.JSONObject

// HU 37: traduce el informe consolidado de Spring a los modelos de la app.
class InformeRepositoryRemoto : InformeRepository {

    override suspend fun obtener(): Resultado<InformeCampanas> {
        val respuesta = InformeApi.obtener().getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching {
            Resultado.Exito(desdeJson(JSONObject(respuesta.cuerpo)))
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    private fun desdeJson(json: JSONObject): InformeCampanas {
        val lista = json.optJSONArray("campanas")
        val campanas = buildList {
            if (lista != null) {
                for (indice in 0 until lista.length()) {
                    add(campanaDesde(lista.getJSONObject(indice)))
                }
            }
        }
        return InformeCampanas(
            empresa = texto(json, "empresa"),
            total = json.optInt("total", campanas.size),
            aprobados = json.optInt("aprobados", 0),
            dudosos = json.optInt("dudosos", 0),
            rechazados = json.optInt("rechazados", 0),
            campanas = campanas
        )
    }

    private fun campanaDesde(json: JSONObject): CampanaInforme = CampanaInforme(
        id = json.optLong("id").takeIf { it > 0 },
        fecha = texto(json, "fecha"),
        redSocial = RedSocial.desde(texto(json, "redSocial")),
        oferta = texto(json, "oferta"),
        estado = texto(json, "estado").ifBlank { "APROBADO" }
    )

    private fun texto(json: JSONObject, clave: String): String =
        if (json.isNull(clave)) "" else json.optString(clave)

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
            else -> "No se pudo cargar tu panel de consulta. Intenta de nuevo."
        }
    }
}
