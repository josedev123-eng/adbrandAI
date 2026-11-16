package com.adbrand.core.notificaciones

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.adbrand.data.repository.NotificacionesRepository
import com.adbrand.data.repository.NotificacionesRepositoryRemoto
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

// HU 50: une Firebase con el backend. Sin un proyecto de Firebase configurado
// (firebase.xml con google_app_id) la app funciona igual, solo no pide token:
// así el equipo puede desarrollar sin credenciales.
object GestorPush {

    private const val ETIQUETA = "GestorPush"
    private val repositorio: NotificacionesRepository = NotificacionesRepositoryRemoto()

    fun crearCanales(contexto: Context) = CanalesNotificacion.crear(contexto)

    // HU 50: el permiso se pide en Android 13+; en versiones anteriores ya viene dado.
    fun tienePermiso(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Pide el token a Firebase y lo registra en el backend. Si el teléfono no
     * tiene Firebase configurado, no hace nada (no rompe el inicio de sesión).
     */
    suspend fun sincronizar(contexto: Context) {
        val token = tokenFirebase(contexto) ?: return
        val resultado = repositorio.registrar(token, "android", nombreEquipo())
        if (resultado is com.adbrand.core.network.Resultado.Error) {
            Log.w(ETIQUETA, "No se pudo registrar el teléfono: ${resultado.mensaje}")
        }
    }

    suspend fun olvidar(contexto: Context) {
        val token = tokenFirebase(contexto) ?: return
        repositorio.quitar(token)
    }

    suspend fun tokenFirebase(contexto: Context): String? {
        if (app(contexto) == null) {
            Log.i(ETIQUETA, "Firebase no está configurado; se omiten los avisos push.")
            return null
        }
        return try {
            suspendCancellableCoroutine { continuacion ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                    if (tarea.isSuccessful) continuacion.resume(tarea.result) else continuacion.resume(null)
                }
            }
        } catch (error: IllegalStateException) {
            Log.w(ETIQUETA, "Firebase no inicializado: ${error.message}")
            null
        }
    }

    // HU 50: el token puede cambiar; se vuelve a enviar al backend sin pedir nada.
    suspend fun sincronizarTokenNuevo(token: String) {
        repositorio.registrar(token, "android", nombreEquipo())
    }

    private fun app(contexto: Context): FirebaseApp? {
        FirebaseApp.getApps(contexto).firstOrNull()?.let { return it }
        return try {
            // Lee google_app_id, gcm_defaultSenderId, etc. de res/values/firebase.xml.
            val opciones = FirebaseOptions.fromResource(contexto) ?: return null
            FirebaseApp.initializeApp(contexto, opciones)
        } catch (error: IllegalStateException) {
            null
        }
    }

    private fun nombreEquipo(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android" }
}
