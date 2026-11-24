package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.FirebaseAuthApi

// Registro desde la app con Firebase Authentication: Firebase crea el acceso y manda el correo de verificación;
// Spring crea la empresa (la misma cuenta que se usa en la web). Recuperar la contraseña sigue con el código de Spring.
// Sin Firebase configurado en el teléfono, el registro es el de siempre.
class AccesoCuentaRepositoryFirebase(
    private val firebase: ProveedorFirebase = FirebaseAuthApi(),
    private val servidor: ServidorFirebase = ServidorFirebaseSpring(),
    private val spring: AccesoCuentaRepository = AccesoCuentaRepositoryRemoto()
) : AccesoCuentaRepository by spring {

    override suspend fun registrar(nombre: String, empresa: String, correo: String, contrasena: String): Resultado<String> {
        if (!firebase.disponible()) return spring.registrar(nombre, empresa, correo, contrasena)
        val limpio = correo.trim()

        when (val creado = firebase.crearCuenta(limpio, contrasena)) {
            ResultadoFirebase.Ok -> Unit
            ResultadoFirebase.CorreoYaRegistrado ->
                return Resultado.Error("Ese correo ya tiene una cuenta. Inicia sesión.", 409)
            ResultadoFirebase.SinConexion -> return Resultado.Error("No se pudo conectar. Revisa tu conexión.")
            ResultadoFirebase.CredencialesInvalidas -> return Resultado.Error("Revisa el correo y la contraseña.", 400)
            is ResultadoFirebase.Error -> return Resultado.Error("No se pudo crear la cuenta: ${creado.mensaje}")
        }

        val token = firebase.idToken()
        val empresaCreada = token?.let { servidor.registrar(it, nombre.trim(), empresa.trim()) }
        if (empresaCreada !is Resultado.Exito) {
            // Si Spring no la creó (por ejemplo, el correo ya era de una cuenta de la web), no se deja el acceso a medias.
            firebase.eliminarActual()
            firebase.salir()
            return if (empresaCreada is Resultado.Error && empresaCreada.codigo == 409) {
                Resultado.Error("Ese correo ya tiene una cuenta. Inicia sesión con tu contraseña de siempre.", 409)
            } else {
                Resultado.Error((empresaCreada as? Resultado.Error)?.mensaje ?: "No se pudo crear la cuenta. Intenta de nuevo.")
            }
        }

        firebase.enviarVerificacion()
        firebase.salir()
        return Resultado.Exito(
            "¡Cuenta creada! Te enviamos un correo a $limpio para confirmarlo (revisa también el correo no deseado). " +
                "Ábrelo y luego inicia sesión."
        )
    }
}
