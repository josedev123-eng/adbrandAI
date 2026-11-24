package com.adbrand.domain.model

// HU 58: publicaciones del calendario, separadas por red social, y las próximas que van a salir.
data class MetricasPublicaciones(
    val programadas: Long = 0,
    val publicadas: Long = 0,
    val canceladas: Long = 0,
    val porRed: List<PublicacionesPorRed> = emptyList(),
    val proximas: List<ProximaPublicacion> = emptyList()
)

// Programadas y publicadas de una red social, por ejemplo redSocial = "INSTAGRAM".
data class PublicacionesPorRed(val redSocial: String, val programadas: Long, val publicadas: Long)

// Una de las siguientes publicaciones programadas. La campaña viene vacía si no tiene.
data class ProximaPublicacion(
    val id: Long,
    val titulo: String,
    val redSocial: String,
    val fecha: String,
    val hora: String,
    val campanaNombre: String? = null,
    val campanaColor: String? = null
)
