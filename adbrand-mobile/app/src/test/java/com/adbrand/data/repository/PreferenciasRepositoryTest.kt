package com.adbrand.data.repository

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// HU 51: las preferencias se leen con todo activo por defecto y respetan los
// interruptores que ya guardó la empresa.
class PreferenciasRepositoryTest {

    private val repositorio = PreferenciasRepositoryRemoto()

    @Test
    fun leeTodasLasPreferenciasActivas() {
        val json = JSONObject(
            """{"campanas": true, "suscripciones": true, "novedades": true, "todasActivas": true}"""
        )

        val preferencias = repositorio.preferenciasDesde(json)

        assertTrue(preferencias.campanas)
        assertTrue(preferencias.suscripciones)
        assertTrue(preferencias.novedades)
        assertTrue(preferencias.todasActivas)
    }

    @Test
    fun leeLosInterruptoresApagados() {
        val json = JSONObject(
            """{"campanas": false, "suscripciones": true, "novedades": false, "todasActivas": false}"""
        )

        val preferencias = repositorio.preferenciasDesde(json)

        assertFalse(preferencias.campanas)
        assertTrue(preferencias.suscripciones)
        assertFalse(preferencias.novedades)
        assertFalse(preferencias.todasActivas)
    }

    @Test
    fun unaRespuestaSinCamposMantieneTodoActivo() {
        val preferencias = repositorio.preferenciasDesde(JSONObject("{}"))

        assertTrue(preferencias.campanas)
        assertTrue(preferencias.suscripciones)
        assertTrue(preferencias.novedades)
        assertTrue(preferencias.todasActivas)
        assertTrue(preferencias.resumen.contains("todos"))
    }
}
