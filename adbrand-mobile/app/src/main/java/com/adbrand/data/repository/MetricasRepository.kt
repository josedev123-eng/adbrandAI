package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.RangoFechas

// HU 57 y 58: contrato para probar la pantalla de métricas sin red.
// HU 60: las dos métricas se piden para un periodo (sin fechas, todo).
interface MetricasRepository {

    suspend fun contenido(rango: RangoFechas): Resultado<MetricasContenido>

    suspend fun publicaciones(rango: RangoFechas): Resultado<MetricasPublicaciones>
}
