package com.adbrand.data.repository

// Firebase Authentication visto desde la app: una interfaz para que los repositorios se prueben sin Firebase real.
// La implementación de verdad está en data/remote/FirebaseAuthApi.kt.
interface ProveedorFirebase {

    // false si el teléfono no tiene Firebase configurado (sin google-services.json): la app usa el login de Spring.
    fun disponible(): Boolean

    suspend fun entrar(correo: String, contrasena: String): ResultadoFirebase

    suspend fun crearCuenta(correo: String, contrasena: String): ResultadoFirebase

    // Vuelve a preguntar a Firebase si el usuario actual ya confirmó su correo.
    suspend fun correoVerificado(): Boolean

    // ID token del usuario actual, para mandarlo a Spring. null si no hay usuario o falló.
    suspend fun idToken(): String?

    suspend fun enviarVerificacion()

    // Borra el usuario recién creado (si Spring no pudo crear la empresa, para no dejar cuentas a medias).
    suspend fun eliminarActual()

    fun salir()
}

sealed interface ResultadoFirebase {
    data object Ok : ResultadoFirebase
    data object CredencialesInvalidas : ResultadoFirebase
    data object CorreoYaRegistrado : ResultadoFirebase
    data object SinConexion : ResultadoFirebase
    data class Error(val mensaje: String) : ResultadoFirebase
}
