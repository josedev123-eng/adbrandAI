package com.adbrand.ui.previsualizacion.recorte

// HU 47: el asunto (lo usa el correo) y el texto que acompañan a la imagen al compartirla (WhatsApp lo pone como mensaje).
data class MensajeCompartir(val asunto: String, val cuerpo: String) {

    companion object {
        // HU 61: si la marca tiene eslogan, la firma lo lleva ("— Panadería Rosa: “El sabor de tu barrio”").
        fun para(nombreNegocio: String, textoAnuncio: String, eslogan: String = ""): MensajeCompartir {
            val negocio = nombreNegocio.ifBlank { "mi negocio" }
            return MensajeCompartir(
                asunto = "Anuncio de $negocio",
                cuerpo = buildString {
                    append(textoAnuncio.trim())
                    if (isNotEmpty()) append("\n\n")
                    append("— $negocio")
                    val lema = eslogan.trim().trim('"', '“', '”')
                    if (lema.isNotEmpty()) append(": “$lema”")
                }
            )
        }
    }
}
