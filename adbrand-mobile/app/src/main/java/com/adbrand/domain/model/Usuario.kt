package com.adbrand.domain.model

// HU 42: datos mínimos del usuario que devuelve el login.
data class Usuario(
    val id: Long,
    val nombre: String,
    val nombreEmpresa: String,
    val correo: String
)
