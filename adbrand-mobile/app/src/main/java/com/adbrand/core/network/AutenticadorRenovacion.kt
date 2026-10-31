package com.adbrand.core.network

import com.adbrand.core.session.AlmacenSesion
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

// HU 43: cuando Spring responde 401, pide una sola vez un token nuevo y reintenta.
// Si la renovación falla (token vencido o revocado), limpia la sesión y deja que la app vuelva al login.
class AutenticadorRenovacion(
    private val renovador: RenovadorSesion,
    private val sesion: AlmacenSesion
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val peticion = response.request

        // Sin Bearer no hay nada que renovar (por ejemplo, el propio login).
        if (peticion.header("Authorization") == null) return null

        // Evita un ciclo si el endpoint de renovación también responde 401.
        val ruta = peticion.url.encodedPath
        if (ruta.endsWith("/auth/login") || ruta.endsWith("/auth/renovar")) return null

        // Solo se reintenta una vez por cadena de respuestas.
        if (contarRespuestas(response) >= 2) {
            sesion.limpiar()
            return null
        }

        val tokenRenovacion = sesion.tokenRenovacion()
        if (tokenRenovacion.isNullOrBlank()) {
            sesion.limpiar()
            return null
        }

        val nuevos = renovador.renovar(tokenRenovacion) ?: run {
            sesion.limpiar()
            return null
        }

        sesion.guardarSesion(nuevos.token, nuevos.tokenRenovacion)
        return peticion.newBuilder()
            .header("Authorization", "Bearer ${nuevos.token}")
            .build()
    }

    private fun contarRespuestas(response: Response): Int {
        var actual: Response? = response
        var total = 0
        while (actual != null) {
            total++
            actual = actual.priorResponse
        }
        return total
    }
}
