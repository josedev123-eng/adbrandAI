package com.adbrand.core.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.adbrand.R

// HU 50, 49 y 52: un canal por tipo de aviso, con nombre y descripción visibles
// en los ajustes del teléfono. Así el usuario puede silenciar solo una categoría.
object CanalesNotificacion {

    const val SISTEMA = "adbrand_sistema"
    const val CAMPANAS = "adbrand_campanas"
    const val SUSCRIPCION = "adbrand_suscripcion"

    fun crear(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = contexto.getSystemService(NotificationManager::class.java) ?: return

        gestor.createNotificationChannel(
            NotificationChannel(SISTEMA, "Avisos del sistema", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Novedades y avisos importantes de AdBrand."
                enableLights(true)
            }
        )
        gestor.createNotificationChannel(
            NotificationChannel(CAMPANAS, "Recordatorios de campañas", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Te avisamos cuando una campaña empieza o termina."
            }
        )
        gestor.createNotificationChannel(
            NotificationChannel(SUSCRIPCION, "Vencimiento de tu plan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos para renovar tu suscripción a tiempo."
            }
        )
    }

    // HU 49 y 52: el tipo que manda el backend elige el canal (y su importancia).
    fun canalPara(tipo: String?): String = when (tipo) {
        TipoAviso.CAMPANA -> CAMPANAS
        TipoAviso.SUSCRIPCION -> SUSCRIPCION
        else -> SISTEMA
    }

    fun nombreVisible(tipo: String?): String = when (tipo) {
        TipoAviso.CAMPANA -> "Recordatorio de campaña"
        TipoAviso.SUSCRIPCION -> "Vencimiento de tu plan"
        else -> "AdBrand"
    }

    @Suppress("unused")
    private val icono = R.drawable.ic_notificacion
}

// Los mismos valores que envía Spring en el payload de datos.
object TipoAviso {
    const val SISTEMA = "system_alert"
    const val CAMPANA = "campaign_reminder"
    const val SUSCRIPCION = "subscription_reminder"
}
