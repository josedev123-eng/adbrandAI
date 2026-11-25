package com.adbrand.data.remote

import com.adbrand.data.repository.ProveedorFirebase
import com.adbrand.data.repository.ResultadoFirebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.tasks.await

// Firebase Authentication (lo que pide el curso de móviles): crear cuenta, entrar, verificar el correo y salir.
// Firebase confirma quién es el usuario; luego la app le pasa el ID token a Spring (/api/auth/firebase),
// que devuelve la sesión de siempre. Ver AuthRepositoryFirebase.
class FirebaseAuthApi : ProveedorFirebase {

    // Si el teléfono no tiene google-services.json, Firebase no se inicia y getInstance() falla.
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    override fun disponible(): Boolean = auth != null

    override suspend fun entrar(correo: String, contrasena: String): ResultadoFirebase =
        ejecutar { requireNotNull(auth).signInWithEmailAndPassword(correo, contrasena).await() }

    override suspend fun crearCuenta(correo: String, contrasena: String): ResultadoFirebase =
        ejecutar { requireNotNull(auth).createUserWithEmailAndPassword(correo, contrasena).await() }

    override suspend fun correoVerificado(): Boolean {
        val usuario = auth?.currentUser ?: return false
        runCatching { usuario.reload().await() }
        return auth.currentUser?.isEmailVerified == true
    }

    // true fuerza un token nuevo: así lleva "email_verified" actualizado después de confirmar el correo.
    override suspend fun idToken(): String? =
        runCatching { auth?.currentUser?.getIdToken(true)?.await()?.token }.getOrNull()

    override suspend fun enviarVerificacion() {
        runCatching { auth?.currentUser?.sendEmailVerification()?.await() }
    }

    override suspend fun eliminarActual() {
        runCatching { auth?.currentUser?.delete()?.await() }
    }

    override fun salir() {
        auth?.signOut()
    }

    private suspend fun ejecutar(accion: suspend () -> Unit): ResultadoFirebase = try {
        accion()
        ResultadoFirebase.Ok
    } catch (error: FirebaseAuthUserCollisionException) {
        ResultadoFirebase.CorreoYaRegistrado
    } catch (error: FirebaseAuthInvalidUserException) {
        ResultadoFirebase.CredencialesInvalidas
    } catch (error: FirebaseAuthInvalidCredentialsException) {
        ResultadoFirebase.CredencialesInvalidas
    } catch (error: FirebaseNetworkException) {
        ResultadoFirebase.SinConexion
    } catch (error: Exception) {
        ResultadoFirebase.Error(error.message ?: "No se pudo conectar con Firebase.")
    }
}
