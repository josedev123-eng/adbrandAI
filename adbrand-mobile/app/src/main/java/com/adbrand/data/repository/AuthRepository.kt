package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.domain.model.Usuario

// HU 42: el ViewModel depende de esta interfaz, así las pruebas usan un repositorio falso.
interface AuthRepository {

    suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario>

    suspend fun cerrarSesion(): Resultado<Unit>
}
