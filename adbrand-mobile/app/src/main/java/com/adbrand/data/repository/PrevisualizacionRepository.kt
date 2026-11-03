package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.Previsualizacion

// HU 45: contrato para probar la pantalla sin red.
interface PrevisualizacionRepository {

    suspend fun obtener(): Resultado<Previsualizacion>
}
