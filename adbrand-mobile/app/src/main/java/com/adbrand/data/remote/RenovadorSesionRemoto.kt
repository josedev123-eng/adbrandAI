package com.adbrand.data.remote

import com.adbrand.core.network.RenovadorSesion
import com.adbrand.core.network.TokensRenovados
import org.json.JSONObject

// HU 43: llama a POST /api/auth/renovar con el cliente sin sesión para no repetir el 401.
class RenovadorSesionRemoto : RenovadorSesion {

    override fun renovar(tokenRenovacion: String): TokensRenovados? {
        val respuesta = AuthApi.renovar(tokenRenovacion).getOrNull() ?: return null
        if (!respuesta.esExitosa) return null
        return runCatching {
            val json = JSONObject(respuesta.cuerpo)
            TokensRenovados(
                token = json.getString("token"),
                tokenRenovacion = json.getString("tokenRenovacion")
            )
        }.getOrNull()
    }
}
