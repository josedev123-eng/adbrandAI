package com.adbrand.core.notificaciones

import android.content.Context
import android.content.SharedPreferences

// HU 50: recuerda si ya se pidió el permiso de avisos para no insistir en cada
// apertura. No es información sensible: no usa el almacén cifrado de la sesión.
object MemoriaAvisos {

    private const val ARCHIVO = "adbrand_avisos"
    private const val CLAVE_PREGUNTADO = "permiso_preguntado"

    private fun preferencias(contexto: Context): SharedPreferences =
        contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun yaPreguntado(contexto: Context): Boolean =
        preferencias(contexto).getBoolean(CLAVE_PREGUNTADO, false)

    fun marcarPreguntado(contexto: Context) {
        preferencias(contexto).edit().putBoolean(CLAVE_PREGUNTADO, true).apply()
    }
}
