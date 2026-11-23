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

    // Listo para publicar: el texto con hashtags (marca + rubro + Perú) y el enlace "Pide por WhatsApp".
    // Por defecto falla, así las pruebas con datos falsos no tienen que implementarlo.
    suspend fun textoParaPublicar(contenidoId: Long): Resultado<String> = Resultado.Error("No disponible")
}
