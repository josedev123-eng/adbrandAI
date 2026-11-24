package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones

// HU 57 y 58: contrato para probar la pantalla de métricas sin red.
interface MetricasRepository {

    suspend fun contenido(): Resultado<MetricasContenido>

    suspend fun publicaciones(): Resultado<MetricasPublicaciones>
}
