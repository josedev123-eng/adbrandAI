package com.adbrand.ui.previsualizacion.recorte

// HU 47: el asunto (lo usa el correo) y el texto que acompañan a la imagen al compartirla (WhatsApp lo pone como mensaje).
data class MensajeCompartir(val asunto: String, val cuerpo: String) {

    companion object {
        fun para(nombreNegocio: String, textoAnuncio: String): MensajeCompartir {
            val negocio = nombreNegocio.ifBlank { "mi negocio" }
            return MensajeCompartir(
                asunto = "Anuncio de $negocio",
                cuerpo = buildString {
                    append(textoAnuncio.trim())
                    if (isNotEmpty()) append("\n\n")
                    append("— $negocio")
                }
            )
        }
    }
}
