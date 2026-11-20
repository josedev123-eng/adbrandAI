package com.adbrand.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// HU 52: el plan vigente de la empresa, tal como lo devuelve /api/suscripciones/mia.
// Si no hay suscripción, estado es SIN_PLAN y la pantalla muestra solo el catálogo.
data class MiSuscripcion(
    val plan: String = "",
    val estado: String = "SIN_PLAN",
    val fechaVencimiento: String = "",
    val renovacionAutomatica: Boolean = false,
    val diasRestantes: Long = 0,
    val monto: Double = 0.0
) {
    val tienePlan: Boolean
        get() = plan.isNotBlank() && !estado.equals("SIN_PLAN", ignoreCase = true)

    val vencePronto: Boolean
        get() = tienePlan && diasRestantes in 0..3

    val estadoEtiqueta: String
        get() = when (estado.uppercase()) {
            "ACTIVA" -> "Activa"
            "VENCIDA" -> "Vencida"
            "PENDIENTE_PAGO" -> "Pago pendiente"
            "CANCELADA" -> "Cancelada"
            else -> "Sin plan"
        }

    val renovacionTexto: String
        get() = if (renovacionAutomatica) "Renovación automática" else "Renovación manual"

    val renovacionDetalle: String
        get() = if (renovacionAutomatica) {
            "Se cobra sola antes del vencimiento. Revisa tu método de pago."
        } else {
            "Tienes que pagar antes del vencimiento para no perder tus campañas."
        }

    val fechaVencimientoLegible: String
        get() = runCatching { LocalDate.parse(fechaVencimiento).format(FORMATO) }
            .getOrDefault(fechaVencimiento)

    val vencimientoTexto: String
        get() = when {
            !tienePlan -> "Sin vencimiento"
            diasRestantes < 0 -> "Venció hace ${-diasRestantes} días"
            diasRestantes == 0L -> "Vence hoy"
            diasRestantes == 1L -> "Vence mañana"
            else -> "Vence en $diasRestantes días"
        }

    val montoLegible: String
        get() = if (monto > 0) "S/ %.2f".format(Locale.forLanguageTag("es-PE"), monto) else ""
}

// HU 52: un plan del catálogo público (/api/planes).
data class PlanDisponible(
    val id: Long = 0,
    val nombre: String = "",
    val precio: Double = 0.0,
    val limiteTokens: Int = 0,
    val descripcion: String = ""
) {
    val precioLegible: String
        get() = "S/ %.2f".format(Locale.forLanguageTag("es-PE"), precio)

    val tokensLegibles: String
        get() = when {
            limiteTokens >= 1_000_000 -> "${limiteTokens / 1_000_000} M tokens"
            limiteTokens >= 1_000 -> "${limiteTokens / 1_000} K tokens"
            else -> "$limiteTokens tokens"
        }
}

private val FORMATO: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"))
