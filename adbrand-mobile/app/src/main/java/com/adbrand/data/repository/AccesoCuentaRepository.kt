package com.adbrand.data.repository

import com.adbrand.core.network.Resultado

interface AccesoCuentaRepository {
    suspend fun registrar(nombre: String, empresa: String, correo: String, contrasena: String): Resultado<String>
    suspend fun solicitarCodigo(correo: String): Resultado<String>
    suspend fun cambiarContrasena(correo: String, codigo: String, contrasena: String): Resultado<String>
}
