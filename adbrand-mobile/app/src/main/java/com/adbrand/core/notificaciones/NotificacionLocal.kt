package com.adbrand.core.notificaciones

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adbrand.MainActivity
import com.adbrand.R

// HU 50: notificación local con la identidad de la app. Se usa con la app en
// primer plano y con mensajes de solo datos (data-only), donde Android no la
// dibuja solo. Al tocarla se abre la pantalla que corresponde al tipo de aviso.
object NotificacionLocal {

    @SuppressLint("MissingPermission")
    fun mostrar(contexto: Context, aviso: AvisoPush) {
        if (!GestorPush.tienePermiso(contexto)) return

        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AvisosPush.EXTRA_TIPO, aviso.tipo)
            putExtra(AvisosPush.EXTRA_TITULO, aviso.titulo)
            putExtra(AvisosPush.EXTRA_CUERPO, aviso.cuerpo)
            aviso.campanaId?.let { putExtra(AvisosPush.EXTRA_CAMPANA, it.toString()) }
            aviso.plan?.let { putExtra(AvisosPush.EXTRA_PLAN, it) }
        }
        val pendiente = PendingIntent.getActivity(
            contexto,
            aviso.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val importante = aviso.tipo == TipoAviso.SUSCRIPCION
        val notificacion = NotificationCompat.Builder(contexto, CanalesNotificacion.canalPara(aviso.tipo))
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(contexto, R.color.tinta_verde))
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.cuerpo))
            .setSubText(CanalesNotificacion.nombreVisible(aviso.tipo))
            .setPriority(
                if (importante) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()

        runCatching {
            NotificationManagerCompat.from(contexto).notify(aviso.id.toInt(), notificacion)
        }
    }
}
