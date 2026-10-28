package com.adbrand.core.network

// Respuesta cruda de OkHttp: código y cuerpo. Los repositorios la convierten a modelos.
data class RespuestaHttp(val codigo: Int, val cuerpo: String) {

    val esExitosa: Boolean
        get() = codigo in 200..299
}
