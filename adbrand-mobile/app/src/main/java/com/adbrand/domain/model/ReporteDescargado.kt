package com.adbrand.domain.model

import java.io.File

// HU 59: el PDF del informe ya guardado en el celular, listo para abrirlo o compartirlo.
data class ReporteDescargado(val nombre: String, val archivo: File)
