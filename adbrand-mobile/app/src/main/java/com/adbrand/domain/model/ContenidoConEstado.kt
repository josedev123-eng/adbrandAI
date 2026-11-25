package com.adbrand.domain.model

// HU 56: un contenido con el estado que devuelve GET /api/contenido/estados.
// estadoActual: PENDIENTE, APROBADO, RECHAZADO, EN_REVISION o RECHAZADO_MODERADOR.
data class ContenidoConEstado(
    val id: Long,
    val oferta: String = "",
    val redSocial: String = "INSTAGRAM",
    val estadoActual: String = "PENDIENTE",
    val motivo: String = "",
    // El titular que escribió la IA (se muestra en vez del pedido del dueño). Vacío en respuestas viejas.
    val titular: String = "",
    // HU 63: el texto del anuncio y el día en que se creó (AAAA-MM-DD), para buscar.
    val texto: String = "",
    val fecha: String = ""
) {
    // Lo canceló el dueño en AdBrand (antes "rechazado por ti"): va en "Cancelados" y se puede eliminar.
    val cancelado: Boolean get() = estadoActual == "RECHAZADO"

    // El texto de la etiqueta de color.
    val etiqueta: String
        get() = when (estadoActual) {
            "APROBADO" -> "Aprobado"
            "RECHAZADO" -> "Cancelado"
            "EN_REVISION" -> "En revisión"
            "RECHAZADO_MODERADOR" -> "Rechazado por el moderador"
            else -> "Pendiente"
        }
}
