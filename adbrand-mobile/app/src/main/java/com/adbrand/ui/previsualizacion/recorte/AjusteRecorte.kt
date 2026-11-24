package com.adbrand.ui.previsualizacion.recorte

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// HU 48: zoom y recorte del anuncio dentro del marco del formato. Lógica pura (sin Android) para poder probarla.
//
// La imagen siempre cubre todo el marco (como "recortar para llenar"). Con zoom 1 se ve la parte central;
// al acercar, el recorte se achica; al arrastrar, el recorte se mueve, sin salirse nunca de la imagen.
// desplazamientoX/Y: cuánto se movió el centro del recorte desde el centro de la imagen, en píxeles de la imagen.
data class AjusteRecorte(
    val escala: Float = 1f,
    val desplazamientoX: Float = 0f,
    val desplazamientoY: Float = 0f
) {
    val esOriginal: Boolean get() = escala == 1f && desplazamientoX == 0f && desplazamientoY == 0f

    companion object {
        const val ESCALA_MINIMA = 1f
        const val ESCALA_MAXIMA = 4f
    }
}

// Rectángulo en píxeles de la imagen original que se ve dentro del marco.
data class RectanguloRecorte(val izquierda: Int, val arriba: Int, val ancho: Int, val alto: Int)

object Recorte {

    // Tamaño del recorte con zoom 1: el más grande con la proporción del marco que entra en la imagen.
    private fun tamanoBase(anchoImagen: Int, altoImagen: Int, proporcionMarco: Float): Pair<Float, Float> {
        val proporcionImagen = anchoImagen.toFloat() / altoImagen
        return if (proporcionImagen > proporcionMarco) {
            altoImagen * proporcionMarco to altoImagen.toFloat()
        } else {
            anchoImagen.toFloat() to anchoImagen / proporcionMarco
        }
    }

    // C1: aplica un gesto (pellizco y arrastre). zoom > 1 acerca; arrastre en píxeles de la pantalla.
    // Mover el dedo hacia la derecha mueve la imagen a la derecha (el recorte va a la izquierda).
    fun aplicarGesto(
        actual: AjusteRecorte,
        zoom: Float,
        arrastreX: Float,
        arrastreY: Float,
        anchoMarcoPantalla: Float,
        anchoImagen: Int,
        altoImagen: Int,
        proporcionMarco: Float
    ): AjusteRecorte {
        if (anchoImagen <= 0 || altoImagen <= 0 || anchoMarcoPantalla <= 0f) return actual
        val escala = (actual.escala * zoom).coerceIn(AjusteRecorte.ESCALA_MINIMA, AjusteRecorte.ESCALA_MAXIMA)
        val (anchoBase, _) = tamanoBase(anchoImagen, altoImagen, proporcionMarco)
        val pixelesImagenPorPantalla = (anchoBase / escala) / anchoMarcoPantalla
        return limitar(
            AjusteRecorte(
                escala = escala,
                desplazamientoX = actual.desplazamientoX - arrastreX * pixelesImagenPorPantalla,
                desplazamientoY = actual.desplazamientoY - arrastreY * pixelesImagenPorPantalla
            ),
            anchoImagen, altoImagen, proporcionMarco
        )
    }

    // Mantiene el recorte dentro de la imagen (no se ven bordes vacíos).
    fun limitar(ajuste: AjusteRecorte, anchoImagen: Int, altoImagen: Int, proporcionMarco: Float): AjusteRecorte {
        val (anchoBase, altoBase) = tamanoBase(anchoImagen, altoImagen, proporcionMarco)
        val maxX = max(0f, (anchoImagen - anchoBase / ajuste.escala) / 2f)
        val maxY = max(0f, (altoImagen - altoBase / ajuste.escala) / 2f)
        return ajuste.copy(
            desplazamientoX = ajuste.desplazamientoX.coerceIn(-maxX, maxX),
            desplazamientoY = ajuste.desplazamientoY.coerceIn(-maxY, maxY)
        )
    }

    // Lo que se dibuja en el marco y lo que se guarda al compartir.
    fun rectangulo(ajuste: AjusteRecorte, anchoImagen: Int, altoImagen: Int, proporcionMarco: Float): RectanguloRecorte {
        val limitado = limitar(ajuste, anchoImagen, altoImagen, proporcionMarco)
        val (anchoBase, altoBase) = tamanoBase(anchoImagen, altoImagen, proporcionMarco)
        val ancho = (anchoBase / limitado.escala).roundToInt().coerceIn(1, anchoImagen)
        val alto = (altoBase / limitado.escala).roundToInt().coerceIn(1, altoImagen)
        val centroX = anchoImagen / 2f + limitado.desplazamientoX
        val centroY = altoImagen / 2f + limitado.desplazamientoY
        val izquierda = (centroX - ancho / 2f).roundToInt().coerceIn(0, anchoImagen - ancho)
        val arriba = (centroY - alto / 2f).roundToInt().coerceIn(0, altoImagen - alto)
        return RectanguloRecorte(izquierda, arriba, min(ancho, anchoImagen), min(alto, altoImagen))
    }
}
