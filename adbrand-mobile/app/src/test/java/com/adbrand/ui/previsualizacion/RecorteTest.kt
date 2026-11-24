package com.adbrand.ui.previsualizacion

import com.adbrand.ui.previsualizacion.recorte.AjusteRecorte
import com.adbrand.ui.previsualizacion.recorte.Recorte
import com.adbrand.ui.previsualizacion.recorte.RectanguloRecorte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// HU 48: zoom con dos dedos, recorte dentro del marco del formato y restablecer, sin Android.
class RecorteTest {

    // Imagen de 1000 x 1000 px dibujada en un marco de 500 px de ancho.
    private fun gesto(actual: AjusteRecorte, zoom: Float, x: Float = 0f, y: Float = 0f, proporcion: Float = 1f) =
        Recorte.aplicarGesto(actual, zoom, x, y, anchoMarcoPantalla = 500f, anchoImagen = 1000, altoImagen = 1000, proporcionMarco = proporcion)

    @Test
    fun sinZoomSeVeLaImagenEnteraEnUnMarcoCuadrado() {
        assertEquals(RectanguloRecorte(0, 0, 1000, 1000), Recorte.rectangulo(AjusteRecorte(), 1000, 1000, 1f))
    }

    @Test
    fun alAcercarElRecorteSeAchicaYQuedaCentrado() {
        val ajuste = gesto(AjusteRecorte(), zoom = 2f)

        assertEquals(2f, ajuste.escala)
        assertEquals(RectanguloRecorte(250, 250, 500, 500), Recorte.rectangulo(ajuste, 1000, 1000, 1f))
    }

    @Test
    fun elZoomTieneUnMinimoYUnMaximo() {
        assertEquals(AjusteRecorte.ESCALA_MINIMA, gesto(AjusteRecorte(), zoom = 0.2f).escala)
        assertEquals(AjusteRecorte.ESCALA_MAXIMA, gesto(AjusteRecorte(), zoom = 10f).escala)
    }

    @Test
    fun alArrastrarElRecorteSeMuevePeroNuncaSaleDeLaImagen() {
        val acercado = gesto(AjusteRecorte(), zoom = 2f)
        // Mover el dedo 100 px a la derecha mueve la imagen a la derecha: el recorte va hacia la izquierda.
        val movido = gesto(acercado, zoom = 1f, x = 100f)
        assertTrue(Recorte.rectangulo(movido, 1000, 1000, 1f).izquierda < 250)

        // Un arrastre enorme se queda en el borde de la imagen.
        val alBorde = gesto(acercado, zoom = 1f, x = 99_999f, y = -99_999f)
        val recorte = Recorte.rectangulo(alBorde, 1000, 1000, 1f)
        assertEquals(0, recorte.izquierda)
        assertEquals(500, recorte.arriba)
    }

    @Test
    fun elRecorteTieneLaFormaDelMarcoDelFormatoElegido() {
        val vertical = Recorte.rectangulo(AjusteRecorte(), 1000, 1000, 1080f / 1920f)
        val horizontal = Recorte.rectangulo(AjusteRecorte(), 1000, 1000, 1200f / 628f)

        assertEquals(1000, vertical.alto)
        assertEquals(1080f / 1920f, vertical.ancho.toFloat() / vertical.alto, 0.01f)
        assertEquals(1000, horizontal.ancho)
        assertEquals(1200f / 628f, horizontal.ancho.toFloat() / horizontal.alto, 0.01f)
    }

    @Test
    fun restablecerEsVolverAlAjusteOriginal() {
        val ajustado = gesto(gesto(AjusteRecorte(), zoom = 3f), zoom = 1f, x = 50f, y = 20f)
        assertTrue(!ajustado.esOriginal)
        assertTrue(AjusteRecorte().esOriginal)
    }
}
