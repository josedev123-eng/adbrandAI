package com.adbrand.ui.anuncio.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.VioletaAdbrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// IA de imágenes (rama de Jose): muestra la imagen que devuelve el backend.
// Las imágenes reales llegan como data URL base64 y se decodifican fuera del hilo
// principal. Las de prueba son SVG: se dibuja una portada de marca con el estilo
// de la opción, sin intentar decodificar un formato que Android no soporta.
@Composable
fun ImagenIa(
    dataUrl: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    val esSimulada = dataUrl.startsWith("data:image/svg+xml", ignoreCase = true)
    var imagen by remember(dataUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(dataUrl) {
        if (!esSimulada && dataUrl.startsWith("data:image/", ignoreCase = true)) {
            imagen = withContext(Dispatchers.IO) { decodificar(dataUrl) }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(IndigoOscuro),
        contentAlignment = Alignment.Center
    ) {
        val actual = imagen
        when {
            actual != null -> androidx.compose.foundation.Image(
                bitmap = actual,
                contentDescription = etiqueta,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            esSimulada || dataUrl.isBlank() -> PortadaSimulada(etiqueta)

            else -> Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Preparando imagen",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "La IA está pintando esta propuesta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PortadaSimulada(etiqueta: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(IndigoAdbrand, VioletaAdbrand)))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "IMAGEN DE PRUEBA",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = etiqueta.ifBlank { "Propuesta de AdBrand" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun decodificar(dataUrl: String): ImageBitmap? = runCatching {
    val base64 = dataUrl.substringAfter("base64,", "")
    if (base64.isBlank()) return null
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    val opciones = BitmapFactory.Options().apply { inSampleSize = 2 }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)?.asImageBitmap()
}.getOrNull()
