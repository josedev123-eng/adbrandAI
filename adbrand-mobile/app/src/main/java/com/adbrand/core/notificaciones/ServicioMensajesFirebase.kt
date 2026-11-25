package com.adbrand.core.notificaciones

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// HU 50: recibe los mensajes de Firebase. Con la app abierta se muestra un banner
// y una notificación local; con la app en segundo plano o cerrada, Android pinta
// la notificación y al tocarla MainActivity recupera el aviso del intent.
class ServicioMensajesFirebase : FirebaseMessagingService() {

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("AvisosAdbrand", "Token de Firebase renovado.")
        CoroutineScope(Dispatchers.IO).launch {
            GestorPush.sincronizarTokenNuevo(token)
        }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        super.onMessageReceived(mensaje)
        val aviso = AvisosPush.desde(
            datos = mensaje.data,
            tituloSistema = mensaje.notification?.title,
            cuerpoSistema = mensaje.notification?.body
        ) ?: return

        BuzonNotificacion.recibir(aviso)
        NotificacionLocal.mostrar(this, aviso)
    }
}
