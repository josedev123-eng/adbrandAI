package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.InformeCampanas

// HU 37: contrato para probar el panel sin red.
interface InformeRepository {

    suspend fun obtener(): Resultado<InformeCampanas>
}
