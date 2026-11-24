package com.adbrand.domain.model

import java.time.LocalDate

// HU 60: las opciones del selector de periodo de las métricas.
enum class OpcionPeriodo(val etiqueta: String) {
    TODO("Todo"),
    ESTE_MES("Este mes"),
    ULTIMOS_3_MESES("Últimos 3 meses"),
    RANGO("Rango propio")
}

// Las fechas que se mandan a Spring (AAAA-MM-DD). En null, ese lado queda abierto.
data class RangoFechas(val desde: String?, val hasta: String?)

object PeriodoMetricas {

    // "Este mes": del día 1 al último día del mes de hoy.
    // "Últimos 3 meses": del día 1 de hace dos meses al último día del mes de hoy (incluye el actual).
    // "Rango propio" usa las fechas que escribió el usuario.
    fun rango(opcion: OpcionPeriodo, hoy: LocalDate, desde: String = "", hasta: String = ""): RangoFechas = when (opcion) {
        OpcionPeriodo.TODO -> RangoFechas(null, null)
        OpcionPeriodo.ESTE_MES -> RangoFechas(
            hoy.withDayOfMonth(1).toString(),
            hoy.withDayOfMonth(hoy.lengthOfMonth()).toString()
        )
        OpcionPeriodo.ULTIMOS_3_MESES -> RangoFechas(
            hoy.minusMonths(2).withDayOfMonth(1).toString(),
            hoy.withDayOfMonth(hoy.lengthOfMonth()).toString()
        )
        OpcionPeriodo.RANGO -> RangoFechas(desde.trim().ifBlank { null }, hasta.trim().ifBlank { null })
    }
}
