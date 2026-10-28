package com.adbrand.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Interceptor

// HU 42: guarda el token de acceso y el de renovación cifrados en el teléfono.
// Es un Singleton porque lo usan el cliente HTTP, el login y el cierre de sesión.
object GestorSesion {

    private const val ARCHIVO = "adbrand_sesion"
    private const val CLAVE_TOKEN = "token_acceso"
    private const val CLAVE_RENOVACION = "token_renovacion"

    @Volatile
    private var preferencias: SharedPreferences? = null

    fun inicializar(contexto: Context) {
        if (preferencias != null) return
        synchronized(this) {
            if (preferencias != null) return
            val claveMaestra = MasterKey.Builder(contexto.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            preferencias = EncryptedSharedPreferences.create(
                contexto.applicationContext,
                ARCHIVO,
                claveMaestra,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun prefs(): SharedPreferences = preferencias
        ?: error("GestorSesion.inicializar(context) debe llamarse antes de usar la sesión.")

    fun guardarSesion(tokenAcceso: String, tokenRenovacion: String?) {
        prefs().edit()
            .putString(CLAVE_TOKEN, tokenAcceso)
            .putString(CLAVE_RENOVACION, tokenRenovacion)
            .apply()
    }

    fun tokenAcceso(): String? = prefs().getString(CLAVE_TOKEN, null)

    fun tokenRenovacion(): String? = prefs().getString(CLAVE_RENOVACION, null)

    fun haySesion(): Boolean = !tokenAcceso().isNullOrBlank()

    fun limpiar() {
        prefs().edit().clear().apply()
    }

    // Interceptor de OkHttp: agrega "Authorization: Bearer ..." a cada llamada autenticada.
    fun interceptorToken(): Interceptor = Interceptor { cadena ->
        val token = tokenAcceso()
        val peticion = if (token.isNullOrBlank()) {
            cadena.request()
        } else {
            cadena.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        cadena.proceed(peticion)
    }
}
