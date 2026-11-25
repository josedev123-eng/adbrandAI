package com.adbrand.data.repository

import com.adbrand.domain.model.MiSuscripcion
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// HU 52: la pantalla del plan lee el vencimiento, el modo de renovación y los
// días restantes sin perder el formato del backend.
class PlanRepositoryTest {

    private val repositorio = PlanRepositoryRemoto()

    @Test
    fun leeElPlanVigenteConRenovacionAutomatica() {
        val json = JSONObject(
            """
            {
              "id": 4,
              "plan": "Emprende",
              "estado": "ACTIVA",
              "fechaInicio": "2026-10-24",
              "fechaVencimiento": "2026-11-23",
              "renovacionAutomatica": true,
              "diasRestantes": 3,
              "monto": 79.90
            }
            """.trimIndent()
        )

        val plan: MiSuscripcion = repositorio.miSuscripcionDesde(json)

        assertTrue(plan.tienePlan)
        assertTrue(plan.renovacionAutomatica)
        assertTrue(plan.vencePronto)
        assertEquals("Emprende", plan.plan)
        assertEquals(3L, plan.diasRestantes)
        assertEquals("23 de noviembre de 2026", plan.fechaVencimientoLegible)
        assertEquals("Vence en 3 días", plan.vencimientoTexto)
        assertEquals("Renovación automática", plan.renovacionTexto)
    }

    @Test
    fun unaRenovacionManualSeAnunciaComoTal() {
        val json = JSONObject(
            """
            {
              "plan": "Crece",
              "estado": "ACTIVA",
              "fechaVencimiento": "2026-11-24",
              "renovacionAutomatica": false,
              "diasRestantes": 1
            }
            """.trimIndent()
        )

        val plan = repositorio.miSuscripcionDesde(json)

        assertFalse(plan.renovacionAutomatica)
        assertEquals("Renovación manual", plan.renovacionTexto)
        assertEquals("Vence mañana", plan.vencimientoTexto)
        assertTrue(plan.vencePronto)
    }

    @Test
    fun sinSuscripcionNoHayPlanNiVencimiento() {
        val json = JSONObject("""{"estado": "SIN_PLAN", "diasRestantes": 0}""")

        val plan = repositorio.miSuscripcionDesde(json)

        assertFalse(plan.tienePlan)
        assertFalse(plan.vencePronto)
        assertEquals("Sin plan", plan.estadoEtiqueta)
        assertEquals("Sin vencimiento", plan.vencimientoTexto)
    }

    @Test
    fun leeElCatalogoDePlanes() {
        val json = JSONObject(
            """{"id": 2, "nombre": "Crece", "precio": 129.9, "limiteTokens": 250000, "descripcion": "Para equipos"}"""
        )

        val plan = repositorio.planDesde(json)

        assertEquals(2L, plan.id)
        assertEquals("Crece", plan.nombre)
        assertEquals("S/ 129.90", plan.precioLegible)
        assertEquals("250 K tokens", plan.tokensLegibles)
    }
}
