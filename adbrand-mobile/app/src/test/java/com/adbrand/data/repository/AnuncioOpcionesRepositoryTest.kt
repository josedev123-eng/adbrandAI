package com.adbrand.data.repository

import com.adbrand.core.network.RespuestaHttp
import com.adbrand.domain.model.RedSocial
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// IA de imágenes (rama de Jose): las respuestas de opciones y versiones se leen
// sin perder imágenes, indicadores de simulación ni el límite de retoques.
class AnuncioOpcionesRepositoryTest {

    private val repositorio = AnuncioOpcionesRepositoryRemoto()

    @Test
    fun leeLasCuatroOpcionesConSuImagen() {
        val json = JSONObject(
            """
            {
              "generacionId": 8,
              "oferta": "2 panetones por S/ 35",
              "redSocial": "INSTAGRAM",
              "opciones": [
                {"id": 1, "numero": 1, "estilo": "Directo", "texto": "Lleva 2 por S/ 35",
                 "imagen": "data:image/png;base64,AAA", "proveedorImagen": "openrouter",
                 "imagenSimulada": false, "textoSimulado": false, "elegida": true},
                {"id": 2, "numero": 2, "estilo": "Cercano", "texto": "Panetón para compartir",
                 "imagen": "data:image/svg+xml;base64,BBB", "proveedorImagen": "simulado",
                 "imagenSimulada": true, "textoSimulado": true, "elegida": false}
              ]
            }
            """.trimIndent()
        )

        val opciones = repositorio.opcionesDesdeJson(json)

        assertEquals(8L, opciones.generacionId)
        assertEquals(RedSocial.INSTAGRAM, opciones.redSocial)
        assertEquals(2, opciones.opciones.size)
        assertEquals("Directo", opciones.opciones[0].estilo)
        assertTrue(opciones.opciones[0].elegida)
        assertTrue(opciones.opciones[1].imagenSimulada)
        assertTrue(opciones.opciones[1].textoSimulado)
        assertFalse(opciones.opciones[1].elegida)
    }

    @Test
    fun leeElHistorialDeVersionesYElLimiteDeRetoques() {
        val json = JSONObject(
            """
            {
              "opcionId": 12,
              "versionActual": 2,
              "imagenActual": "data:image/png;base64,CCC",
              "retoquesRestantes": 19,
              "versiones": [
                {"numero": 1, "instruccion": null, "imagen": "data:image/png;base64,AAA",
                 "proveedorImagen": "simulado", "imagenSimulada": true, "actual": false},
                {"numero": 2, "instruccion": "Fondo más claro", "imagen": "data:image/png;base64,CCC",
                 "proveedorImagen": "openrouter", "imagenSimulada": false, "actual": true}
              ]
            }
            """.trimIndent()
        )

        val historial = repositorio.versionesDesdeJson(json)

        assertEquals(12L, historial.opcionId)
        assertEquals(2, historial.versionActual)
        assertEquals(19, historial.retoquesRestantes)
        assertEquals(2, historial.versiones.size)
        assertEquals("Fondo más claro", historial.versiones[1].instruccion)
        assertTrue(historial.versiones[1].actual)
    }

    @Test
    fun enModoPruebaElLimiteDeRetoquesEsNulo() {
        val json = JSONObject("""{"opcionId": 5, "versionActual": 1, "retoquesRestantes": null, "versiones": []}""")

        assertNull(repositorio.versionesDesdeJson(json).retoquesRestantes)
    }

    @Test
    fun conservaElCodigoDeNegocioDelError() {
        val error = AnuncioJson.error(
            RespuestaHttp(400, """{"codigo":"LIMITE_RETOQUES","mensaje":"Llegaste al límite de retoques del mes."}"""),
            "No se pudo retocar la imagen."
        )

        assertEquals(400, error.codigo)
        assertEquals("LIMITE_RETOQUES", error.codigoError)
        assertEquals("Llegaste al límite de retoques del mes.", error.mensaje)
    }
}
