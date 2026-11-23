package com.adbrand.ui.guia

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.CoralAdbrand
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.VioletaAdbrand

/**
 * "Bran", el robot de AdBrand.AI que explica la app. Dibujado con formas simples (sin imágenes):
 * flota, parpadea, saluda con el brazo y la luz de su antena late. Con los colores de la marca.
 */
@Composable
fun RobotGuia(modifier: Modifier = Modifier, tamano: Dp = 150.dp, saludando: Boolean = true) {
    val animacion = rememberInfiniteTransition(label = "robot")

    // Flota suave hacia arriba y abajo.
    val flotar by animacion.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flotar"
    )
    // Parpadeo: los ojos se cierran un instante cada ~3 segundos.
    val ojos by animacion.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                1f at 0
                1f at 2900
                0.1f at 3000
                1f at 3150
            }
        ),
        label = "parpadeo"
    )
    // Saludo con el brazo derecho.
    val brazo by animacion.animateFloat(
        initialValue = -25f, targetValue = 25f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brazo"
    )
    // Luz de la antena que late.
    val luz by animacion.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "luz"
    )

    Canvas(modifier = modifier.size(tamano)) {
        val w = size.width
        val h = size.height
        val dy = flotar * h * 0.03f

        // Sombra en el piso (más chica cuando sube).
        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(w * 0.30f + flotar * w * 0.02f, h * 0.92f),
            size = Size(w * 0.40f - flotar * w * 0.04f, h * 0.05f)
        )

        // Antena con su luz magenta.
        drawLine(IndigoOscuro, Offset(w * 0.5f, h * 0.13f + dy), Offset(w * 0.5f, h * 0.22f + dy), strokeWidth = w * 0.025f)
        drawCircle(VioletaAdbrand.copy(alpha = luz), radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.11f + dy))
        drawCircle(VioletaAdbrand.copy(alpha = 0.18f * luz), radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.11f + dy))

        // Cabeza.
        drawRoundRect(
            color = IndigoAdbrand,
            topLeft = Offset(w * 0.22f, h * 0.20f + dy),
            size = Size(w * 0.56f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.12f)
        )
        // Pantalla de la cara.
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.29f, h * 0.27f + dy),
            size = Size(w * 0.42f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        // Ojos (se achatan al parpadear).
        val altoOjo = h * 0.07f * ojos
        listOf(0.40f, 0.60f).forEach { x ->
            drawRoundRect(
                color = IndigoOscuro,
                topLeft = Offset(w * x - w * 0.03f, h * 0.345f + dy - altoOjo / 2),
                size = Size(w * 0.06f, altoOjo),
                cornerRadius = CornerRadius(w * 0.03f)
            )
        }
        // Sonrisa.
        drawArc(
            color = IndigoOscuro,
            startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.43f, h * 0.36f + dy),
            size = Size(w * 0.14f, h * 0.07f),
            style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
        )
        // Mejillas coral.
        drawCircle(CoralAdbrand.copy(alpha = 0.5f), radius = w * 0.022f, center = Offset(w * 0.34f, h * 0.42f + dy))
        drawCircle(CoralAdbrand.copy(alpha = 0.5f), radius = w * 0.022f, center = Offset(w * 0.66f, h * 0.42f + dy))

        // Cuerpo.
        drawRoundRect(
            color = IndigoAdbrand,
            topLeft = Offset(w * 0.31f, h * 0.59f + dy),
            size = Size(w * 0.38f, h * 0.27f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        // Botón del pecho.
        drawCircle(VioletaAdbrand, radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.70f + dy))

        // Brazo izquierdo (quieto).
        drawLine(IndigoOscuro, Offset(w * 0.31f, h * 0.65f + dy), Offset(w * 0.20f, h * 0.76f + dy), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
        // Brazo derecho: saluda.
        val hombro = Offset(w * 0.69f, h * 0.64f + dy)
        rotate(degrees = if (saludando) brazo - 40f else 30f, pivot = hombro) {
            drawLine(IndigoOscuro, hombro, Offset(hombro.x + w * 0.16f, hombro.y), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
            drawCircle(CoralAdbrand, radius = w * 0.035f, center = Offset(hombro.x + w * 0.18f, hombro.y))
        }
    }
}
