package com.adbrand.core.notificaciones

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// HU 50: Firebase renueva el token cada tanto (reinstalación, limpieza de datos,
// cambio de equipo). Aquí se reenvía al backend sin molestar al usuario.
class ServicioMensajesFirebase : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("AvisosAdbrand", "Token de Firebase renovado.")
        CoroutineScope(Dispatchers.IO).launch {
            GestorPush.sincronizarTokenNuevo(token)
        }
    }
}
