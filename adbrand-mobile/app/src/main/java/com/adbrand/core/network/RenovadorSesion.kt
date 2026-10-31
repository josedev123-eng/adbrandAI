package com.adbrand.core.network

// HU 43: contrato para renovar la sesión. Las pruebas usan un renovador falso.
data class TokensRenovados(val token: String, val tokenRenovacion: String)

interface RenovadorSesion {
    fun renovar(tokenRenovacion: String): TokensRenovados?
}
