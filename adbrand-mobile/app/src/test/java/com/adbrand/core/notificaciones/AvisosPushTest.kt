package com.adbrand.core.notificaciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

// HU 50: el payload del backend se interpreta sin perder el tipo ni el enlace.
class AvisosPushTest {

    @Test
    fun unAvisoDelSistemaUsaElTituloEnviado() {
        val aviso = AvisosPush.desde(
            mapOf(
                "type" to TipoAviso.SISTEMA,
                AvisosPush.EXTRA_TITULO to "Aviso de prueba",
                AvisosPush.EXTRA_CUERPO to "Todo funciona."
            )
        )

        assertEquals("Aviso de prueba", aviso?.titulo)
        assertEquals("Todo funciona.", aviso?.cuerpo)
        assertEquals(TipoAviso.SISTEMA, aviso?.tipo)
        assertEquals("panel", aviso?.let { RutasAviso.destino(it) })
    }

    @Test
    fun unMensajeSinTipoNoEsUnAviso() {
        assertNull(AvisosPush.desde(mapOf("campaign_id" to "4")))
    }

    @Test
    fun elTipoDeCampanaAbreLaFichaDeSuCampana() {
        val aviso = AvisosPush.desde(
            mapOf(
                "type" to TipoAviso.CAMPANA,
                "campaign_id" to "12",
                AvisosPush.EXTRA_TITULO to "Mañana empieza tu campaña"
            )
        )

        assertEquals(TipoAviso.CAMPANA, aviso?.tipo)
        assertEquals(12L, aviso?.campanaId)
        assertEquals("campana/12", aviso?.let { RutasAviso.destino(it) })
    }

    @Test
    fun unaCampanaSinIdentificadorNoTieneDestino() {
        val aviso = AvisosPush.desde(mapOf("type" to TipoAviso.CAMPANA))

        assertNull(aviso?.campanaId)
        assertNull(aviso?.let { RutasAviso.destino(it) })
    }

    @Test
    fun elTipoDeSuscripcionTraeElPlan() {
        val aviso = AvisosPush.desde(
            mapOf(
                "type" to TipoAviso.SUSCRIPCION,
                "plan" to "Emprende"
            )
        )

        assertEquals("Emprende", aviso?.plan)
        assertEquals("Tu plan vence pronto", aviso?.titulo)
    }

    @Test
    fun cadaAvisoRecibeUnIdentificadorPropio() {
        val primero = AvisosPush.desde(mapOf("type" to TipoAviso.SISTEMA))
        val segundo = AvisosPush.desde(mapOf("type" to TipoAviso.SISTEMA))

        assertNotEquals(primero?.id, segundo?.id)
    }
}
