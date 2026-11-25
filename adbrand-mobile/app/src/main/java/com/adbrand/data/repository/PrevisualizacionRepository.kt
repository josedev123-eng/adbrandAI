package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.Previsualizacion

// HU 45: contrato para probar la pantalla sin red.
interface PrevisualizacionRepository {

    // HU 46: los anuncios ajustados al formato elegido.
    suspend fun obtener(formato: String = "CUADRADO"): Resultado<Previsualizacion>

    // HU 46: imagen del anuncio ("data:image/...;base64,...") o null si no tiene.
    suspend fun imagen(contenidoId: Long): Resultado<String?>

    // HU 47: registrar en el backend que se compartió el anuncio.
    suspend fun registrarCompartido(contenidoId: Long, formato: String): Resultado<Unit>

    // HU 62 y 64: el texto con hashtags y "Pide por WhatsApp", y la revisión de reglas de publicidad.
    // Por defecto falla, así las pruebas con datos falsos no tienen que implementarlo.
    suspend fun listoParaPublicar(contenidoId: Long): Resultado<com.adbrand.domain.model.ListoParaPublicar> =
        Resultado.Error("No disponible")

    // HU 65: el video vertical del anuncio (MP4 de 8 s) para Reels, TikTok o Estados de WhatsApp.
    suspend fun video(contenidoId: Long): Resultado<ByteArray> = Resultado.Error("No disponible")
}
