package com.adbrand.domain.model

// HU 41: datos de la empresa que se editan desde "Mi cuenta".
data class Empresa(
    val nombreEmpresa: String = "",
    val correo: String = "",
    val ruc: String = "",
    val razonSocial: String = "",
    val telefono: String = "",
    val direccion: String = ""
)
