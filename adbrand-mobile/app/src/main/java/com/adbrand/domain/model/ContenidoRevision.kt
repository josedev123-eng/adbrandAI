package com.adbrand.domain.model

// HU 53: un contenido en la lista "por revisar" del dueño.
// estado es lo que decidió el filtro o el moderador; puedeAprobar es false si el moderador lo rechazó
// o si todavía lo está revisando.
data class ContenidoRevision(
    val id: Long,
    val oferta: String = "",
    val texto: String = "",
    val redSocial: String = "INSTAGRAM",
    val estado: String = "APROBADO",
    val motivoRevision: String = "",
    val puedeAprobar: Boolean = true,
    val fechaCreacion: String = ""
)
