package com.adbrand.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// HU 49: la ficha de campaña se lee sin perder el color, el tipo ni las fechas.
class CampanaRepositoryTest {

    private val repositorio = CampanaRepositoryRemoto()

    @Test
    fun leeLaCampanaQueAbreElAviso() {
        val json = JSONObject(
            """
            {
              "id": 12,
              "nombre": "Navidad 2026",
              "color": "#C62828",
              "tipo": "CAMPANA",
              "fechaInicio": "2026-12-01",
              "fechaFin": "2026-12-25"
            }
            """.trimIndent()
        )

        val campana = repositorio.campanaDesde(json)

        assertEquals(12L, campana.id)
        assertEquals("Navidad 2026", campana.nombre)
        assertEquals("#C62828", campana.color)
        assertEquals("2026-12-01", campana.fechaInicio)
        assertEquals("2026-12-25", campana.fechaFin)
        assertEquals(25L, campana.duracionDias)
    }

    @Test
    fun unEventoSinColorUsaElColorDeLaMarca() {
        val json = JSONObject(
            """{"id": 3, "nombre": "Feria", "tipo": "EVENTO", "fechaInicio": "2026-11-20", "fechaFin": "2026-11-21"}"""
        )

        val campana = repositorio.campanaDesde(json)

        assertEquals("#1F3D33", campana.color)
        assertEquals(true, campana.esEvento)
        assertEquals("20 de noviembre", campana.fechaInicioLegible)
    }

    @Test
    fun unaFechaVaciaNoRompeLaFicha() {
        val json = JSONObject("""{"id": 4, "nombre": "Sin fechas", "tipo": "CAMPANA"}""")

        val campana = repositorio.campanaDesde(json)

        assertNull(campana.inicio)
        assertEquals("Fechas por confirmar", campana.situacion)
    }
}
