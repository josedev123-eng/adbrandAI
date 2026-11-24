package com.adbrand.data.repository

import java.io.File
import java.io.IOException

// HU 59: guarda los bytes del PDF en una carpeta del celular.
// Criterio 2: solo se crea el archivo si llegó un PDF completo; nunca queda uno vacío o a medias.
object GuardadoPdf {

    // Todo PDF empieza con estos 4 caracteres.
    private val CABECERA_PDF = "%PDF".toByteArray(Charsets.US_ASCII)

    fun esPdf(bytes: ByteArray): Boolean =
        bytes.size > CABECERA_PDF.size && bytes.copyOfRange(0, CABECERA_PDF.size).contentEquals(CABECERA_PDF)

    // Escribe primero en un archivo temporal y lo renombra al final: si algo falla, no queda nada guardado.
    fun guardar(bytes: ByteArray, carpeta: File, nombre: String): File {
        if (!esPdf(bytes)) throw IOException("El archivo recibido no es un PDF.")
        if (!carpeta.exists() && !carpeta.mkdirs()) throw IOException("No se pudo crear la carpeta.")

        val destino = File(carpeta, nombre)
        val temporal = File(carpeta, "$nombre.parcial")
        try {
            temporal.writeBytes(bytes)
            if (destino.exists()) destino.delete()
            if (!temporal.renameTo(destino)) throw IOException("No se pudo guardar el PDF.")
            return destino
        } finally {
            if (temporal.exists()) temporal.delete()
        }
    }
}
