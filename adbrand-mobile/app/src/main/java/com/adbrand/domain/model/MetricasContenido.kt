package com.adbrand.domain.model

// HU 57: cuántos anuncios generó la empresa y en qué estado están.
data class MetricasContenido(
    val total: Long = 0,
    val aprobados: Long = 0,
    val enRevision: Long = 0,
    val rechazados: Long = 0,
    val porMes: List<ContenidoPorMes> = emptyList()
) {
    // Criterio 2: sin contenido se muestran los totales en cero y un aviso.
    val sinContenido: Boolean
        get() = total == 0L
}

// Anuncios generados en un mes, por ejemplo mes = "2026-11".
data class ContenidoPorMes(val mes: String, val cantidad: Long)
