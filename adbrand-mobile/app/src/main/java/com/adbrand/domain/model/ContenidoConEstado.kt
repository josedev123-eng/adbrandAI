package com.adbrand.domain.model

// HU 56: un contenido con el estado que devuelve GET /api/contenido/estados.
// estadoActual: PENDIENTE, APROBADO, RECHAZADO, EN_REVISION o RECHAZADO_MODERADOR.
data class ContenidoConEstado(
    val id: Long,
    val oferta: String = "",
    val redSocial: String = "INSTAGRAM",
    val estadoActual: String = "PENDIENTE",
    val motivo: String = ""
) {
    // El texto de la etiqueta de color.
    val etiqueta: String
        get() = when (estadoActual) {
            "APROBADO" -> "Aprobado"
            "RECHAZADO" -> "Rechazado"
            "EN_REVISION" -> "En revisión"
            "RECHAZADO_MODERADOR" -> "Rechazado por el moderador"
            else -> "Pendiente"
        }
}
