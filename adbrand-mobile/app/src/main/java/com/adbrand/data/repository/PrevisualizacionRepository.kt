package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.Previsualizacion

// HU 45: contrato para probar la pantalla sin red.
interface PrevisualizacionRepository {

    // HU 46: los anuncios ajustados al formato elegido.
    suspend fun obtener(formato: String = "CUADRADO"): Resultado<Previsualizacion>

    // HU 46: imagen del anuncio ("data:image/...;base64,...") o null si no tiene.
    suspend fun imagen(contenidoId: Long): Resultado<String?>
}
