package com.adbrand.core.network

// HU 59: respuesta de OkHttp cuando se descarga un archivo: código y bytes del cuerpo.
class RespuestaArchivo(val codigo: Int, val bytes: ByteArray) {
    val esExitosa: Boolean
        get() = codigo in 200..299

    // Si Spring respondió con error, los bytes son su JSON {codigo, mensaje, campos}.
    val texto: String
        get() = bytes.toString(Charsets.UTF_8)
}
