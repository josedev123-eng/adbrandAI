package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.MetricasContenido

// HU 57: contrato para probar la pantalla de métricas sin red.
interface MetricasRepository {

    suspend fun contenido(): Resultado<MetricasContenido>
}
