package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.PreferenciasApi
import com.adbrand.domain.model.CampoPreferencia
import com.adbrand.domain.model.PreferenciasAvisos
import java.io.IOException
import org.json.JSONObject

// HU 51: lee y guarda las preferencias de avisos que devuelve Spring.
class PreferenciasRepositoryRemoto : PreferenciasRepository {

    override suspend fun obtener(): Resultado<PreferenciasAvisos> {
        val respuesta = PreferenciasApi.obtener().getOrElse { return errorDeRed(it) }
        return leer(respuesta, "No se pudieron cargar tus preferencias.")
    }

    override suspend fun actualizar(
        campo: CampoPreferencia,
        valor: Boolean
    ): Resultado<PreferenciasAvisos> {
        val respuesta = PreferenciasApi.actualizar(campo, valor)
            .getOrElse { return errorDeRed(it) }
        return leer(respuesta, "No se pudo guardar el cambio.")
    }

    private fun leer(respuesta: RespuestaHttp, porDefecto: String): Resultado<PreferenciasAvisos> {
        if (!respuesta.esExitosa) {
            val mensaje = runCatching { JSONObject(respuesta.cuerpo) }
                .getOrNull()
                ?.optString("mensaje")
                ?.takeIf { it.isNotBlank() }
            val final = when {
                mensaje != null -> mensaje
                respuesta.codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
                else -> porDefecto
            }
            return Resultado.Error(final, respuesta.codigo)
        }
        return runCatching {
            Resultado.Exito(preferenciasDesde(JSONObject(respuesta.cuerpo)))
        }.getOrElse {
            Resultado.Error("El servidor devolvió una respuesta inesperada.")
        }
    }

    internal fun preferenciasDesde(json: JSONObject): PreferenciasAvisos {
        val campanas = json.optBoolean("campanas", true)
        val suscripciones = json.optBoolean("suscripciones", true)
        val novedades = json.optBoolean("novedades", true)
        return PreferenciasAvisos(
            campanas = campanas,
            suscripciones = suscripciones,
            novedades = novedades,
            todasActivas = json.optBoolean(
                "todasActivas",
                campanas && suscripciones && novedades
            )
        )
    }

    private fun errorDeRed(error: Throwable): Resultado.Error = Resultado.Error(
        if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
        else "Ocurrió un error inesperado."
    )
}
