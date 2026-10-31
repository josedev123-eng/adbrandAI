package com.adbrand.core.network

import com.adbrand.core.session.AlmacenSesion
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// HU 43: el Authenticator renueva una vez, reintenta y limpia la sesión si el token ya no sirve.
class AutenticadorRenovacionTest {

    private class SesionFalsa(
        var token: String? = "token-viejo",
        var renovacion: String? = "refresh-viejo"
    ) : AlmacenSesion {
        var limpiada = false

        override fun guardarSesion(tokenAcceso: String, tokenRenovacion: String?) {
            token = tokenAcceso
            renovacion = tokenRenovacion
        }

        override fun tokenAcceso(): String? = token

        override fun tokenRenovacion(): String? = renovacion

        override fun haySesion(): Boolean = !token.isNullOrBlank()

        override fun limpiar() {
            limpiada = true
            token = null
            renovacion = null
        }
    }

    private class RenovadorFalso(
        private val resultado: TokensRenovados?
    ) : RenovadorSesion {
        var llamado = false

        override fun renovar(tokenRenovacion: String): TokensRenovados? {
            llamado = true
            return resultado
        }
    }

    private fun respuesta(ruta: String = "/api/cuenta/empresa", conToken: Boolean = true): Response {
        val constructor = Request.Builder().url("http://10.0.2.2:8080$ruta")
        if (conToken) constructor.header("Authorization", "Bearer token-viejo")
        return Response.Builder()
            .request(constructor.build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("No autorizado")
            .build()
    }

    @Test
    fun un401ConTokenVigenteRenuevaYReintentaUnaVez() {
        val sesion = SesionFalsa()
        val renovador = RenovadorFalso(TokensRenovados("token-nuevo", "refresh-nuevo"))
        val autenticador = AutenticadorRenovacion(renovador, sesion)

        val reintento = autenticador.authenticate(null, respuesta())

        assertTrue(renovador.llamado)
        assertEquals("Bearer token-nuevo", reintento?.header("Authorization"))
        assertEquals("token-nuevo", sesion.token)
        assertEquals("refresh-nuevo", sesion.renovacion)
        assertFalse(sesion.limpiada)
    }

    @Test
    fun siElTokenEstaVencidoLimpiaLaSesionYNoReintenta() {
        val sesion = SesionFalsa()
        val autenticador = AutenticadorRenovacion(RenovadorFalso(null), sesion)

        val reintento = autenticador.authenticate(null, respuesta())

        assertNull(reintento)
        assertTrue(sesion.limpiada)
    }

    @Test
    fun siElTokenFueRevocadoLimpiaLaSesionYNoReintenta() {
        val sesion = SesionFalsa()
        val autenticador = AutenticadorRenovacion(RenovadorFalso(null), sesion)

        val reintento = autenticador.authenticate(null, respuesta("/api/negocio/perfil"))

        assertNull(reintento)
        assertTrue(sesion.limpiada)
    }

    @Test
    fun elLoginNoEntraEnUnCicloDeRenovacion() {
        val sesion = SesionFalsa()
        val renovador = RenovadorFalso(TokensRenovados("token-nuevo", "refresh-nuevo"))
        val autenticador = AutenticadorRenovacion(renovador, sesion)

        val reintento = autenticador.authenticate(null, respuesta("/api/auth/login"))

        assertNull(reintento)
        assertFalse(renovador.llamado)
    }

    @Test
    fun noReintentaMasDeUnaVezPorCadena() {
        val sesion = SesionFalsa()
        val renovador = RenovadorFalso(TokensRenovados("token-nuevo", "refresh-nuevo"))
        val autenticador = AutenticadorRenovacion(renovador, sesion)

        val primera = respuesta()
        val segunda = primera.newBuilder().priorResponse(primera).build()
        val reintento = autenticador.authenticate(null, segunda)

        assertNull(reintento)
        assertTrue(sesion.limpiada)
        assertFalse(renovador.llamado)
    }
}
