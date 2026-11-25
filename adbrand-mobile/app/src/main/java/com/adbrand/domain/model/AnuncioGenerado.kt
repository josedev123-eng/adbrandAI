package com.adbrand.domain.model

// Las redes que acepta el enum RedSocial de Spring.
enum class RedSocial(val valor: String, val etiqueta: String, val iniciales: String) {
    INSTAGRAM("INSTAGRAM", "Instagram", "IG"),
    FACEBOOK("FACEBOOK", "Facebook", "FB"),
    WHATSAPP("WHATSAPP", "WhatsApp", "WA");

    companion object {
        fun desde(valor: String): RedSocial =
            entries.firstOrNull { it.valor.equals(valor, ignoreCase = true) } ?: INSTAGRAM
    }
}

// HU 10, 12 y 13: lo que devuelve crear/regenerar un anuncio.
// "simulado" avisa si vino del modo de prueba y "estado" si pasó el filtro automático.
data class AnuncioGenerado(
    val id: Long? = null,
    val texto: String = "",
    val redSocial: RedSocial = RedSocial.INSTAGRAM,
    val tono: Tono = Tono.CERCANO,
    val simulado: Boolean = false,
    val estado: String = "APROBADO",
    val motivoRevision: String = ""
) {
    // HU 13: un anuncio dudoso no se puede copiar para publicarlo todavía.
    val enRevision: Boolean
        get() = estado.equals("DUDOSO", ignoreCase = true)
}
