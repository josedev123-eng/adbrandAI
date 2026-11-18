package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.Campana

// HU 49: contrato para abrir la ficha de una campaña desde un aviso.
interface CampanaRepository {

    suspend fun obtener(id: Long): Resultado<Campana>
}
