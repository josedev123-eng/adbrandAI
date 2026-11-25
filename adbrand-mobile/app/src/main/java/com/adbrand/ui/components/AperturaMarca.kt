package com.adbrand.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.TintaSuave
import com.adbrand.core.theme.VioletaAdbrand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Apertura de marca: la app se presenta como un cuaderno, no como un splash
 * genérico. Todo se dibuja sobre papel y entra en cascada.
 */
@Composable
fun AperturaMarca(
    modifier: Modifier = Modifier,
    duracionMs: Long = 1900L,
    alTerminar: () -> Unit
) {
    val marca = remember { Animatable(0f) }
    val escalaMarca = remember { Animatable(0.88f) }
    val texto = remember { Animatable(0f) }
    val desplazamiento = remember { Animatable(16f) }
    val regla = remember { Animatable(0f) }
    val pie = remember { Animatable(0f) }
    val avance = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { marca.animateTo(1f, tween(480)) }
            launch { escalaMarca.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 210f)) }
            launch {
                delay(140)
                texto.animateTo(1f, tween(520))
            }
            launch {
                delay(140)
                desplazamiento.animateTo(0f, tween(620, easing = FastOutSlowInEasing))
            }
            launch {
                delay(360)
                regla.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
            }
            launch {
                delay(560)
                pie.animateTo(1f, tween(420))
            }
            launch {
                delay(420)
                val restante = (duracionMs - 420L).coerceAtLeast(400L).toInt()
                avance.animateTo(1f, tween(restante, easing = LinearEasing))
            }
        }
        alTerminar()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FondoClaro)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 26.dp)
    ) {
        // Cabecera tipo portada de cuaderno.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(IndigoAdbrand)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ADBRAND",
                style = MaterialTheme.typography.labelSmall,
                color = IndigoOscuro
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "ESTUDIO DE MARCA",
                style = MaterialTheme.typography.labelSmall,
                color = TintaSuave
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BordeSuave)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Marca central: sello, wordmark serif y regla terracota.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        alpha = marca.value
                        scaleX = escalaMarca.value
                        scaleY = escalaMarca.value
                    }
                    .background(IndigoAdbrand),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AB",
                    color = IndigoClaro,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 21.sp
                )
            }

            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = texto.value
                    translationY = desplazamiento.value
                }
            ) {
                Text(
                    text = "AdBrand",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextoPrincipal
                )
                Text(
                    text = "contenido con la voz de tu negocio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .width((78f * regla.value).dp)
                .height(2.dp)
                .background(VioletaAdbrand)
        )

        Spacer(modifier = Modifier.weight(1.15f))

        // Pie: progreso fino y una línea de contexto.
        Column(
            modifier = Modifier.graphicsLayer { alpha = pie.value },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BordeSuave)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(avance.value)
                        .height(1.dp)
                        .background(IndigoAdbrand)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Preparando tu espacio",
                    style = MaterialTheme.typography.labelSmall,
                    color = IndigoOscuro
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "01",
                    style = MaterialTheme.typography.labelSmall,
                    color = TintaSuave,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
