package com.adbrand.data.remote

import com.adbrand.core.network.ClienteApi
import com.adbrand.core.network.RespuestaHttp
import org.json.JSONObject

// HU 42 y HU 43: llamadas de autenticación del backend Spring.
object AuthApi {

    fun iniciarSesion(correo: String, contrasena: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject()
            .put("correo", correo)
            .put("contrasena", contrasena)
            .toString()
        return ClienteApi.ejecutar("POST", "auth/login", cuerpo)
    }

    fun renovar(tokenRenovacion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("tokenRenovacion", tokenRenovacion).toString()
        return ClienteApi.ejecutar("POST", "auth/renovar", cuerpo, ClienteApi.clienteSinSesion)
    }

    fun cerrarSesion(tokenRenovacion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("tokenRenovacion", tokenRenovacion).toString()
        return ClienteApi.ejecutar("POST", "auth/cerrar-sesion", cuerpo)
    }
}
