package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.PerfilApi
import com.adbrand.domain.model.PerfilNegocio
import com.adbrand.domain.model.Tono
import java.io.IOException
import org.json.JSONObject

// HU 44: traduce el perfil del negocio entre Spring y la app.
class PerfilRepositoryRemoto : PerfilRepository {

    override suspend fun obtener(): Resultado<PerfilNegocio> = responder(PerfilApi.obtener())

    override suspend fun guardar(perfil: PerfilNegocio): Resultado<PerfilNegocio> =
        responder(PerfilApi.guardar(perfil))

    private fun responder(llamada: Result<RespuestaHttp>): Resultado<PerfilNegocio> {
        val respuesta = llamada.getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching {
            val json = JSONObject(respuesta.cuerpo)
            Resultado.Exito(
                PerfilNegocio(
                    id = json.optLong("id").takeIf { it > 0 },
                    nombreComercial = json.optString("nombreComercial"),
                    rubro = json.optString("rubro"),
                    publicoObjetivo = json.optString("publicoObjetivo"),
                    tono = runCatching { Tono.valueOf(json.optString("tono")) }.getOrDefault(Tono.CERCANO),
                    descripcion = json.optString("descripcion")
                )
            )
        }.getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
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
            codigo == 404 -> "Todavía no registraste el perfil de tu negocio."
            codigo == 401 -> "Tu sesión venció. Vuelve a iniciar sesión."
            else -> "No se pudo guardar el perfil. Intenta de nuevo."
        }
    }
}
