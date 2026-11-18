package com.adbrand.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// HU 49: una campaña del calendario, tal como la devuelve GET /api/campanas/{id}.
// El aviso de recordatorio trae su id y la app abre esta ficha.
data class Campana(
    val id: Long = 0,
    val nombre: String = "",
    val color: String = "#1F3D33",
    val tipo: String = "CAMPANA",
    val fechaInicio: String = "",
    val fechaFin: String = ""
) {
    val esEvento: Boolean
        get() = tipo.equals("EVENTO", ignoreCase = true)

    val inicio: LocalDate?
        get() = fechaInicio.aFecha()

    val fin: LocalDate?
        get() = fechaFin.aFecha()

    val fechaInicioLegible: String
        get() = inicio?.aTexto() ?: fechaInicio

    val fechaFinLegible: String
        get() = fin?.aTexto() ?: fechaFin

    val duracionDias: Long?
        get() {
            val desde = inicio ?: return null
            val hasta = fin ?: return null
            return ChronoUnit.DAYS.between(desde, hasta) + 1
        }

    val enCurso: Boolean
        get() {
            val hoy = LocalDate.now()
            val desde = inicio ?: return false
            val hasta = fin ?: return false
            return !hoy.isBefore(desde) && !hoy.isAfter(hasta)
        }

    val yaTermino: Boolean
        get() = fin?.isBefore(LocalDate.now()) ?: false

    val diasParaEmpezar: Long?
        get() = inicio?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    val diasParaTerminar: Long?
        get() = fin?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    // Texto corto para la ficha: qué está pasando con la campaña hoy.
    val situacion: String
        get() {
            val diasInicio = diasParaEmpezar ?: return "Fechas por confirmar"
            val diasFin = diasParaTerminar ?: return "Fechas por confirmar"
            return when {
                yaTermino -> "Ya terminó"
                diasInicio == 0L && diasFin == 0L -> "Es hoy"
                diasInicio == 0L -> "Empezó hoy"
                diasInicio == 1L -> "Empieza mañana"
                diasInicio > 1L -> "Empieza en $diasInicio días"
                diasFin == 0L -> "Termina hoy"
                diasFin == 1L -> "Termina mañana"
                enCurso -> "En curso"
                else -> "Programada"
            }
        }
}

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-PE"))

private fun String.aFecha(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

private fun LocalDate.aTexto(): String = format(FORMATO_FECHA)
