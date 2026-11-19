package com.adbrand.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// HU 49: la ficha resume en palabras qué pasa hoy con la campaña del aviso.
class CampanaTest {

    private val hoy = LocalDate.now()

    private fun campana(desde: LocalDate, hasta: LocalDate) = Campana(
        id = 1L,
        nombre = "Campaña",
        fechaInicio = desde.toString(),
        fechaFin = hasta.toString()
    )

    @Test
    fun unaCampanaQueEmpiezaMananaLoIndica() {
        val campana = campana(hoy.plusDays(1), hoy.plusDays(10))

        assertEquals("Empieza mañana", campana.situacion)
        assertFalse(campana.enCurso)
    }

    @Test
    fun unaCampanaQueTerminaMananaLoIndica() {
        val campana = campana(hoy.minusDays(5), hoy.plusDays(1))

        assertEquals("Termina mañana", campana.situacion)
        assertTrue(campana.enCurso)
    }

    @Test
    fun unaCampanaQueYaPasoSeMarcaComoTerminada() {
        val campana = campana(hoy.minusDays(10), hoy.minusDays(2))

        assertEquals("Ya terminó", campana.situacion)
        assertTrue(campana.yaTermino)
    }

    @Test
    fun unaCampanaDeUnSoloDiaCuentaUnDia() {
        val campana = campana(hoy, hoy)

        assertEquals(1L, campana.duracionDias)
        assertEquals("Es hoy", campana.situacion)
    }
}
