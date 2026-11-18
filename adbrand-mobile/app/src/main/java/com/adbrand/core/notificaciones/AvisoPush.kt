package com.adbrand.core.notificaciones

import android.content.Intent
import java.util.concurrent.atomic.AtomicLong

// HU 50, 49 y 52: un aviso ya interpretado, listo para mostrarse en el teléfono
// o en el banner de la app. El backend manda "type" y la referencia que necesita
// la pantalla de destino (campaign_id o plan).
data class AvisoPush(
    val id: Long,
    val titulo: String,
    val cuerpo: String,
    val tipo: String = TipoAviso.SISTEMA,
    val campanaId: Long? = null,
    val plan: String? = null
)

// HU 50: traduce el payload de Firebase o los extras de la notificación local.
// Si el intent no trae un tipo, es una apertura normal de la app y devuelve null.
object AvisosPush {

    const val EXTRA_TIPO = "type"
    const val EXTRA_TITULO = "aviso_titulo"
    const val EXTRA_CUERPO = "aviso_cuerpo"
    const val EXTRA_CAMPANA = "campaign_id"
    const val EXTRA_PLAN = "plan"

    private val contador = AtomicLong(System.currentTimeMillis())

    fun desde(
        datos: Map<String, String>,
        tituloSistema: String? = null,
        cuerpoSistema: String? = null
    ): AvisoPush? {
        val tipo = datos[EXTRA_TIPO]?.takeIf { it.isNotBlank() } ?: return null
        val campanaId = datos[EXTRA_CAMPANA]?.toLongOrNull()
        val plan = datos[EXTRA_PLAN]?.takeIf { it.isNotBlank() }
        return AvisoPush(
            id = contador.incrementAndGet(),
            titulo = datos[EXTRA_TITULO]?.takeIf { it.isNotBlank() }
                ?: tituloSistema?.takeIf { it.isNotBlank() }
                ?: tituloPredeterminado(tipo),
            cuerpo = datos[EXTRA_CUERPO]?.takeIf { it.isNotBlank() }
                ?: cuerpoSistema?.takeIf { it.isNotBlank() }
                ?: cuerpoPredeterminado(tipo),
            tipo = tipo,
            campanaId = campanaId,
            plan = plan
        )
    }

    @Suppress("DEPRECATION")
    fun desde(intent: Intent?): AvisoPush? {
        if (intent == null) return null
        val extras = intent.extras ?: return null
        val datos = extras.keySet().associateWith { clave ->
            extras.getCharSequence(clave)?.toString() ?: extras.get(clave)?.toString() ?: ""
        }
        val tipo = datos[EXTRA_TIPO]?.takeIf { it.isNotBlank() } ?: return null
        val titulo = datos[EXTRA_TITULO]?.takeIf { it.isNotBlank() }
            ?: datos["gcm.n.title"]?.takeIf { it.isNotBlank() }
            ?: datos["gcm.notification.title"]?.takeIf { it.isNotBlank() }
            ?: tituloPredeterminado(tipo)
        val cuerpo = datos[EXTRA_CUERPO]?.takeIf { it.isNotBlank() }
            ?: datos["gcm.n.body"]?.takeIf { it.isNotBlank() }
            ?: datos["gcm.notification.body"]?.takeIf { it.isNotBlank() }
            ?: cuerpoPredeterminado(tipo)
        return AvisoPush(
            id = contador.incrementAndGet(),
            titulo = titulo,
            cuerpo = cuerpo,
            tipo = tipo,
            campanaId = datos[EXTRA_CAMPANA]?.toLongOrNull(),
            plan = datos[EXTRA_PLAN]?.takeIf { it.isNotBlank() }
        )
    }

    fun etiqueta(tipo: String): String = when (tipo) {
        TipoAviso.CAMPANA -> "Campaña"
        TipoAviso.SUSCRIPCION -> "Plan"
        else -> "Aviso"
    }

    private fun tituloPredeterminado(tipo: String): String = when (tipo) {
        TipoAviso.CAMPANA -> "Recordatorio de campaña"
        TipoAviso.SUSCRIPCION -> "Tu plan vence pronto"
        else -> "AdBrand"
    }

    private fun cuerpoPredeterminado(tipo: String): String = when (tipo) {
        TipoAviso.CAMPANA -> "Revisa tu calendario para no perderte el inicio."
        TipoAviso.SUSCRIPCION -> "Renueva a tiempo para seguir publicando."
        else -> "Tienes un aviso nuevo."
    }
}

// HU 49 y 52: cada tipo de aviso abre su pantalla. El aviso de campaña lleva
// el id para abrir su ficha; el del plan se conecta en la HU 52.
object RutasAviso {

    fun destino(aviso: AvisoPush): String? = when (aviso.tipo) {
        TipoAviso.SISTEMA -> "panel"
        TipoAviso.CAMPANA -> aviso.campanaId?.let { "campana/$it" }
        else -> null
    }
}
