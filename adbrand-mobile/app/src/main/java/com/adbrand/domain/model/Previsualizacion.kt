package com.adbrand.domain.model

// HU 45 y 46: el kit más reciente, los anuncios y el formato en que se dibujan en la previsualización.
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
    val logoDiseno: LogoMarca? = null,
    // Identidad de marca (V70): eslogan elegido y propuestas, y el logo como imagen (data URL) y su origen.
    val eslogan: String = "",
    val esloganOpciones: List<String> = emptyList(),
    val logoImagen: String? = null,
    val logoOrigen: String? = null
)

// HU 46: textoAjustado es el texto que entra en el formato elegido; recortado = se acortó con "…".
data class Anuncio(
    val id: Long? = null,
    val texto: String = "",
    val textoAjustado: String = texto,
    val recortado: Boolean = false,
    val redSocial: String = "INSTAGRAM",
    val tono: String = "CERCANO",
    val estado: String = "APROBADO",
    val fechaCreacion: String = ""
)

// HU 46: formato de la previsualización (cuadrado = publicación, vertical = historia, horizontal).
data class FormatoVista(
    val codigo: String = "CUADRADO",
    val nombre: String = "Cuadrado",
    val uso: String = "Publicación",
    val ancho: Int = 1080,
    val alto: Int = 1080,
    val maxCaracteres: Int = 125
)

data class Previsualizacion(
    val kit: KitMarca? = null,
    val anuncios: List<Anuncio> = emptyList(),
    val formato: FormatoVista = FormatoVista()
)
