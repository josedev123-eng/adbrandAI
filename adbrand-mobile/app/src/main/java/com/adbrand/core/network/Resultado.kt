package com.adbrand.core.network

// Resultado de una llamada al backend. Evita lanzar excepciones hasta la pantalla.
sealed interface Resultado<out T> {

    data class Exito<T>(val dato: T) : Resultado<T>

    // codigo es el estado HTTP; codigoError es el código de negocio que envía Spring
    // (PERFIL_NO_ENCONTRADO, IA_NO_DISPONIBLE, ...) y campos los errores por campo de un 400.
    data class Error(
        val mensaje: String,
        val codigo: Int? = null,
        val codigoError: String? = null,
        val campos: Map<String, String> = emptyMap()
    ) : Resultado<Nothing>
}
