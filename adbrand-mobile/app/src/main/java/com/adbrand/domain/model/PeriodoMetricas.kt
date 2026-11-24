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

    // HU 60, criterio 2: las mismas reglas que revisa Spring (FECHA_INVALIDA y FECHAS_INVALIDAS).
    // Devuelve el aviso a mostrar, o null si el rango se puede aplicar. Una fecha vacía deja ese lado abierto.
    fun validar(desde: String, hasta: String): String? {
        val inicio = fecha(desde)
        val fin = fecha(hasta)
        if (inicio == FECHA_MAL_ESCRITA || fin == FECHA_MAL_ESCRITA) {
            return "Escribe las fechas como AAAA-MM-DD."
        }
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            return "La fecha de inicio no puede ser posterior a la fecha de fin."
        }
        return null
    }

    // Marca una fecha que no se pudo leer (distinta de "vacía", que es null).
    private val FECHA_MAL_ESCRITA = LocalDate.MIN

    private fun fecha(texto: String): LocalDate? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return null
        return runCatching { LocalDate.parse(limpio) }.getOrDefault(FECHA_MAL_ESCRITA)
    }
}
