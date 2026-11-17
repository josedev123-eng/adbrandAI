package com.adbrand.data.repository

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.AuthApi
import com.adbrand.data.remote.FirebaseAuthApi
import com.adbrand.domain.model.Usuario
import org.json.JSONObject

// Lo que la app le pide a Spring con un token de Firebase. Interfaz para probar sin red.
interface ServidorFirebase {
    suspend fun iniciarSesion(idToken: String): Resultado<Usuario>
    suspend fun registrar(idToken: String, nombre: String, empresa: String): Resultado<String>
    suspend fun vincular(idToken: String): Resultado<Unit>
}

class ServidorFirebaseSpring(private val remoto: AuthRepositoryRemoto = AuthRepositoryRemoto()) : ServidorFirebase {

    override suspend fun iniciarSesion(idToken: String): Resultado<Usuario> =
        remoto.leerSesion(AuthApi.iniciarSesionFirebase(idToken).getOrElse { return remoto.sinConexion(it) })

    override suspend fun registrar(idToken: String, nombre: String, empresa: String): Resultado<String> {
        val respuesta = AuthApi.registrarFirebase(idToken, nombre, empresa).getOrElse { return remoto.sinConexion(it) }
        if (respuesta.esExitosa) return Resultado.Exito("ok")
        val mensaje = runCatching { JSONObject(respuesta.cuerpo).optString("mensaje") }.getOrNull()
        return Resultado.Error(mensaje?.takeIf { it.isNotBlank() } ?: "No se pudo crear la cuenta.", respuesta.codigo)
    }

    override suspend fun vincular(idToken: String): Resultado<Unit> {
        val respuesta = AuthApi.vincularFirebase(idToken).getOrElse { return remoto.sinConexion(it) }
        return if (respuesta.esExitosa) Resultado.Exito(Unit) else Resultado.Error("No se pudo unir Firebase.", respuesta.codigo)
    }
}

// Inicio de sesión de la app con Firebase Authentication. La cuenta y los datos son los mismos de la web:
// 1. Firebase revisa el correo y la contraseña y Spring entrega la sesión de siempre (con el ID token de Firebase).
// 2. Si Firebase todavía no conoce esa cuenta (se creó en la web), se entra con la contraseña de Spring y,
//    en el mismo momento, se le crea su acceso en Firebase y se une ("migración automática"; la próxima vez ya es paso 1).
// 3. Una cuenta nueva del móvil entra recién cuando confirma su correo (Firebase manda el enlace).
// Sin Firebase configurado en el teléfono, funciona como antes: solo con Spring.
class AuthRepositoryFirebase(
    private val firebase: ProveedorFirebase = FirebaseAuthApi(),
    private val spring: AuthRepository = AuthRepositoryRemoto(),
    private val servidor: ServidorFirebase = ServidorFirebaseSpring()
) : AuthRepository {

    override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario> {
        val limpio = correo.trim()
        if (!firebase.disponible()) return spring.iniciarSesion(limpio, contrasena)

        return when (val resultado = firebase.entrar(limpio, contrasena)) {
            ResultadoFirebase.Ok -> conFirebase(limpio, contrasena)
            ResultadoFirebase.CredencialesInvalidas -> migrarDesdeLaWeb(limpio, contrasena)
            ResultadoFirebase.SinConexion -> Resultado.Error("No se pudo conectar. Revisa tu conexión.")
            ResultadoFirebase.CorreoYaRegistrado, is ResultadoFirebase.Error -> spring.iniciarSesion(limpio, contrasena)
        }
    }

    private suspend fun conFirebase(correo: String, contrasena: String): Resultado<Usuario> {
        if (!firebase.correoVerificado()) {
            // Una cuenta que vino de la web todavía puede entrar con su contraseña de siempre mientras confirma el correo.
            val deLaWeb = spring.iniciarSesion(correo, contrasena)
            if (deLaWeb is Resultado.Exito) return deLaWeb
            firebase.enviarVerificacion()
            firebase.salir()
            return Resultado.Error(
                "Confirma tu correo: te enviamos un enlace a $correo (revisa también el correo no deseado). Luego vuelve a entrar.",
                403
            )
        }
        val token = firebase.idToken() ?: return Resultado.Error("No se pudo confirmar tu sesión con Firebase. Intenta de nuevo.")
        val sesion = servidor.iniciarSesion(token)
        if (sesion is Resultado.Error) firebase.salir()
        return sesion
    }

    private suspend fun migrarDesdeLaWeb(correo: String, contrasena: String): Resultado<Usuario> {
        val deLaWeb = spring.iniciarSesion(correo, contrasena)
        if (deLaWeb !is Resultado.Exito) return deLaWeb

        // Ya entró con Spring; crear su acceso en Firebase no debe impedir que use la app si algo falla.
        if (firebase.crearCuenta(correo, contrasena) == ResultadoFirebase.Ok) {
            firebase.idToken()?.let { servidor.vincular(it) }
            firebase.enviarVerificacion()
        }
        return deLaWeb
    }

    override suspend fun cerrarSesion(): Resultado<Unit> {
        firebase.salir()
        return spring.cerrarSesion()
    }
}
