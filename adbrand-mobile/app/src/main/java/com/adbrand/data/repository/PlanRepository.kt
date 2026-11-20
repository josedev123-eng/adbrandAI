package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.MiSuscripcion
import com.adbrand.domain.model.PlanDisponible

// HU 52: contrato para probar la pantalla del plan sin red.
interface PlanRepository {

    suspend fun mia(): Resultado<MiSuscripcion>

    suspend fun planes(): Resultado<List<PlanDisponible>>
}
