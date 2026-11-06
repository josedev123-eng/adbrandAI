package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.CuentaApi
import com.adbrand.domain.model.Empresa
import java.io.IOException
import org.json.JSONObject

// HU 41: traduce los datos de la empresa entre Spring y la app.
class CuentaRepositoryRemoto : CuentaRepository {

    override suspend fun obtener(): Resultado<Empresa> = responder(CuentaApi.obtener())

    override suspend fun guardar(empresa: Empresa): Resultado<Empresa> =
        responder(CuentaApi.guardar(empresa))

    private fun responder(llamada: Result<com.adbrand.core.network.RespuestaHttp>): Resultado<Empresa> {
        val respuesta = llamada.getOrElse { return errorDeRed(it) }
        if (!respuesta.esExitosa) {
            return Resultado.Error(mensajeDeError(respuesta.codigo, respuesta.cuerpo), respuesta.codigo)
        }
        return runCatching {
            val json = JSONObject(respuesta.cuerpo)
            Resultado.Exito(
                Empresa(
                    nombreEmpresa = texto(json, "nombreEmpresa"),
                    correo = texto(json, "correo"),
                    ruc = texto(json, "ruc"),
                    razonSocial = texto(json, "razonSocial"),
                    telefono = texto(json, "telefono"),
                    direccion = texto(json, "direccion")
                )
            )
        }.getOrElse { Resultado.Error("El servidor devolvió una respuesta inesperada.") }
    }

    private fun texto(json: JSONObject, clave: String): String = if (json.isNull(clave)) "" else json.optString(clave)

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
            else -> "No se pudieron guardar los datos. Intenta de nuevo."
        }
    }
}
