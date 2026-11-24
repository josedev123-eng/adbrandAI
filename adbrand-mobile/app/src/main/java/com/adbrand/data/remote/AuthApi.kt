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

    // Firebase Authentication: Spring verifica el ID token de Firebase y devuelve la sesión de siempre.
    suspend fun iniciarSesionFirebase(idToken: String): Result<RespuestaHttp> = ClienteApi.ejecutarAsync(
        "POST", "auth/firebase", JSONObject().put("idToken", idToken).toString(), ClienteApi.clienteSinSesion
    )

    suspend fun registrarFirebase(idToken: String, nombre: String, nombreEmpresa: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("idToken", idToken).put("nombre", nombre).put("nombreEmpresa", nombreEmpresa).toString()
        return ClienteApi.ejecutarAsync("POST", "auth/firebase/registro", cuerpo, ClienteApi.clienteSinSesion)
    }

    // Con la sesión de siempre (ya guardada): une la cuenta de la web con su usuario de Firebase recién creado.
    suspend fun vincularFirebase(idToken: String): Result<RespuestaHttp> = ClienteApi.ejecutarAsync(
        "POST", "cuenta/firebase/vincular", JSONObject().put("idToken", idToken).toString()
    )

    fun renovar(tokenRenovacion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("tokenRenovacion", tokenRenovacion).toString()
        return ClienteApi.ejecutar("POST", "auth/renovar", cuerpo, ClienteApi.clienteSinSesion)
    }

    suspend fun cerrarSesion(tokenRenovacion: String): Result<RespuestaHttp> {
        val cuerpo = JSONObject().put("tokenRenovacion", tokenRenovacion).toString()
        return ClienteApi.ejecutarAsync("POST", "auth/cerrar-sesion", cuerpo)
    }
}
