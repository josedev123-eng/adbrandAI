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

    // HU 66: video con IA (Premium). La IA anima la foto del anuncio; tarda 1 a 3 minutos.
    suspend fun disponibilidadVideoIa(): Resultado<com.adbrand.domain.model.DisponibilidadVideoIa> =
        Resultado.Error("No disponible")
    suspend fun iniciarVideoIa(contenidoId: Long): Resultado<com.adbrand.domain.model.VideoIa> = Resultado.Error("No disponible")
    suspend fun consultarVideoIa(videoId: Long): Resultado<com.adbrand.domain.model.VideoIa> = Resultado.Error("No disponible")
    suspend fun descargarVideoIa(videoId: Long): Resultado<ByteArray> = Resultado.Error("No disponible")

    // Ajuste 14: el anuncio diseñado (JPG) con la plantilla de la opción elegida, la marca y el formato pedido.
    suspend fun imagenFinal(contenidoId: Long, formato: String): Resultado<ByteArray> = Resultado.Error("No disponible")
}
