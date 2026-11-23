package com.adbrand.domain.model

import java.time.LocalDate

// HU 63: una fecha peruana que vende, con su idea de anuncio según el rubro y cuántos días faltan.
data class FechaSugerida(val fecha: LocalDate, val nombre: String, val idea: String, val diasFaltan: Long)
