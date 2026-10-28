package com.adbrand.core.network

// Resultado de una llamada al backend. Evita lanzar excepciones hasta la pantalla.
sealed interface Resultado<out T> {

    data class Exito<T>(val dato: T) : Resultado<T>

    data class Error(val mensaje: String, val codigo: Int? = null) : Resultado<Nothing>
}
