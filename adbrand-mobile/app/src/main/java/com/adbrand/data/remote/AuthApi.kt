package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import org.json.JSONObject

// HU 42 y HU 43: llamadas de autenticación del backend Spring.
object AuthApi {

    suspend fun registrar(nombre: String, nombreEmpresa: String, correo: String, contrasena: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("nombre", nombre).put("nombreEmpresa", nombreEmpresa)
            .put("correo", correo).put("contrasena", contrasena).toString()
        return ClienteApi.ejecutarAsync("POST", "auth/registro", cuerpo, ClienteApi.clienteSinSesion)
    }

    suspend fun solicitarCodigo(correo: String): Result<RespuestaHttp> = ClienteApi.ejecutarAsync(
        "POST", "auth/recuperar/solicitar", JSONObject().put("correo", correo).toString(), ClienteApi.clienteSinSesion
    )

    suspend fun cambiarContrasena(correo: String, codigo: String, contrasenaNueva: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("correo", correo).put("codigo", codigo)
            .put("contrasenaNueva", contrasenaNueva).toString()
        return ClienteApi.ejecutarAsync("POST", "auth/recuperar/cambiar", cuerpo, ClienteApi.clienteSinSesion)
    }

    suspend fun iniciarSesion(correo: String, contrasena: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("correo", correo)
            .put("contrasena", contrasena)
            .toString()
        return ClienteApi.ejecutarAsync("POST", "auth/login", cuerpo, ClienteApi.clienteSinSesion)
    }

    fun renovar(tokenRenovacion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("tokenRenovacion", tokenRenovacion).toString()
        return ClienteApi.ejecutar("POST", "auth/renovar", cuerpo, ClienteApi.clienteSinSesion)
    }

    suspend fun cerrarSesion(tokenRenovacion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("tokenRenovacion", tokenRenovacion).toString()
        return ClienteApi.ejecutarAsync("POST", "auth/cerrar-sesion", cuerpo)
    }
}
