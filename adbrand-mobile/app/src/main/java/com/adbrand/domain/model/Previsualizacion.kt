package com.adbrand.domain.model

// HU 45: el kit más reciente y los anuncios que se dibujan en la previsualización.
data class LogoMarca(
    val texto: String = "",
    val forma: String = "REDONDEADO",
    val colorFondo: String = "#4F46E5",
    val colorTexto: String = "#FFFFFF",
    val tipografia: String = ""
)

data class KitMarca(
    val id: Long? = null,
    val nombreNegocio: String = "",
    val sector: String = "",
    val logoConcepto: String = "",
    val tipografiaPrimaria: String = "",
    val tipografiaSecundaria: String = "",
    val paletaColores: String = "",
    val vozMarca: String = "",
    val logoDiseno: LogoMarca? = null
)

data class Anuncio(
    val id: Long? = null,
    val texto: String = "",
    val redSocial: String = "INSTAGRAM",
    val tono: String = "CERCANO",
    val estado: String = "APROBADO",
    val fechaCreacion: String = ""
)

data class Previsualizacion(
    val kit: KitMarca? = null,
    val anuncios: List<Anuncio> = emptyList()
)
