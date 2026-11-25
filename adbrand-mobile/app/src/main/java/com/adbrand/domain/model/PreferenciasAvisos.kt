package com.adbrand.domain.model

// HU 51: los tres interruptores de avisos que puede cambiar la empresa.
enum class CampoPreferencia(val clave: String, val titulo: String, val detalle: String) {
    CAMPANAS(
        clave = "campanas",
        titulo = "Recordatorios de campañas",
        detalle = "Te avisamos el día antes de que empiece o termine una campaña."
    ),
    SUSCRIPCIONES(
        clave = "suscripciones",
        titulo = "Vencimiento de tu plan",
        detalle = "Tres días antes de que venza o se renueve tu suscripción."
    ),
    NOVEDADES(
        clave = "novedades",
        titulo = "Novedades de AdBrand",
        detalle = "Ideas, mejoras y consejos para sacar más partido a la IA."
    )
}

// HU 51: preferencias de avisos de la empresa. Si el backend no tiene fila
// guardada devuelve todo activo, igual que este valor por defecto.
data class PreferenciasAvisos(
    val campanas: Boolean = true,
    val suscripciones: Boolean = true,
    val novedades: Boolean = true,
    val todasActivas: Boolean = true
) {

    fun valor(campo: CampoPreferencia): Boolean = when (campo) {
        CampoPreferencia.CAMPANAS -> campanas
        CampoPreferencia.SUSCRIPCIONES -> suscripciones
        CampoPreferencia.NOVEDADES -> novedades
    }

    fun con(campo: CampoPreferencia, nuevo: Boolean): PreferenciasAvisos {
        val actualizado = when (campo) {
            CampoPreferencia.CAMPANAS -> copy(campanas = nuevo)
            CampoPreferencia.SUSCRIPCIONES -> copy(suscripciones = nuevo)
            CampoPreferencia.NOVEDADES -> copy(novedades = nuevo)
        }
        return actualizado.copy(
            todasActivas = actualizado.campanas &&
                actualizado.suscripciones &&
                actualizado.novedades
        )
    }

    val resumen: String
        get() = when {
            todasActivas -> "Recibes todos los avisos."
            !campanas && !suscripciones && !novedades -> "No recibirás ningún aviso."
            else -> "Solo recibirás los avisos que dejaste activos."
        }
}
