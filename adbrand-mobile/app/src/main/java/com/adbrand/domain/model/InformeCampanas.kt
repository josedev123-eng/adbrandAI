package com.adbrand.domain.model

// HU 37: una fila del informe consolidado. Cada anuncio generado cuenta como campaña.
data class CampanaInforme(
    val id: Long? = null,
    val fecha: String = "",
    val redSocial: RedSocial = RedSocial.INSTAGRAM,
    val oferta: String = "",
    val estado: String = "APROBADO"
)

// HU 37: totales por estado y el detalle de las campañas de la empresa.
data class InformeCampanas(
    val empresa: String = "",
    val total: Int = 0,
    val aprobados: Int = 0,
    val dudosos: Int = 0,
    val rechazados: Int = 0,
    val campanas: List<CampanaInforme> = emptyList()
)
