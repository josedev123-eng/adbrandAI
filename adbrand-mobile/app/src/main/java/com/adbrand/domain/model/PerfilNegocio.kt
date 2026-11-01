package com.adbrand.domain.model

// HU 44: perfil del negocio. El correo y el teléfono se muestran junto al perfil,
// pero viven en la cuenta de la empresa.
data class PerfilNegocio(
    val id: Long? = null,
    val nombreComercial: String = "",
    val rubro: String = "",
    val publicoObjetivo: String = "",
    val tono: Tono = Tono.CERCANO,
    val descripcion: String = "",
    val correo: String = "",
    val telefono: String = ""
)
