package com.adbrand.ui.previsualizacion.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.KitMarca
import com.adbrand.domain.model.LogoMarca
import com.adbrand.ui.previsualizacion.viewmodel.PrevisualizacionViewModel

// HU 45: aplica el logo, los colores y la tipografía del kit. Si no hay kit, muestra un aviso.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrevisualizacionScreen(
    viewModel: PrevisualizacionViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Previsualización") }) }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (estado.cargando) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            estado.error?.let { mensaje ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(mensaje, color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(onClick = viewModel::cargar, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            val kit = estado.kit
            if (!estado.cargando && estado.cargado) {
                if (kit == null) {
                    item { AvisoSinKit() }
                } else {
                    item { EncabezadoKit(kit) }
                    item { MuestraTipografia(kit) }
                }

                if (estado.anuncios.isEmpty()) {
                    item {
                        Text(
                            text = "Todavía no generaste anuncios.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "Tus publicaciones",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(estado.anuncios, key = { it.id ?: it.hashCode().toLong() }) { anuncio ->
                        TarjetaAnuncio(anuncio, kit)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoSinKit() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Aún no tienes kit de marca", style = MaterialTheme.typography.titleMedium)
            Text(
                "Genera tu kit para que las publicaciones usen tu logo, tus colores y tu tipografía.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EncabezadoKit(kit: KitMarca) {
    val colores = coloresDe(kit.paletaColores)
    val principal = colores.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val secundario = colores.getOrNull(1) ?: MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoMarca(kit.logoDiseno, kit.nombreNegocio, principal)
                Column {
                    Text(
                        text = kit.nombreNegocio.ifBlank { "Tu negocio" },
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = familiaDe(kit.tipografiaPrimaria)
                    )
                    Text(
                        text = kit.sector,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (colores.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colores.take(5).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape8)
                                .background(color)
                        )
                    }
                }
            }

            Text(
                text = "Color principal de tu marca",
                style = MaterialTheme.typography.labelLarge,
                color = principal
            )
            Text(
                text = "Acento",
                style = MaterialTheme.typography.labelLarge,
                color = secundario
            )

            if (kit.vozMarca.isNotBlank()) {
                Text(
                    text = "Voz de marca: ${kit.vozMarca}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MuestraTipografia(kit: KitMarca) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Así se ve tu tipografía", style = MaterialTheme.typography.labelLarge)
            Text(
                text = kit.nombreNegocio.ifBlank { "Tu marca" },
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = familiaDe(kit.tipografiaPrimaria),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = kit.logoConcepto.ifBlank { "Un anuncio con tu identidad visual." },
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = familiaDe(kit.tipografiaSecundaria)
            )
        }
    }
}

@Composable
private fun TarjetaAnuncio(anuncio: Anuncio, kit: KitMarca?) {
    val colores = coloresDe(kit?.paletaColores.orEmpty())
    val acento = colores.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val colorEstado = when (anuncio.estado) {
        "APROBADO" -> Color(0xFF15803D)
        "DUDOSO" -> Color(0xFFB45309)
        "RECHAZADO" -> Color(0xFFB91C1C)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = anuncio.redSocial,
                    style = MaterialTheme.typography.labelLarge,
                    color = acento
                )
                Text(
                    text = anuncio.estado,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorEstado
                )
            }
            Text(
                text = anuncio.texto,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = familiaDe(kit?.tipografiaSecundaria.orEmpty())
            )
            Text(
                text = "Tono ${anuncio.tono.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogoMarca(logo: LogoMarca?, nombre: String, colorPrincipal: Color) {
    val texto = logo?.texto?.ifBlank { null } ?: nombre.take(2).uppercase().ifBlank { "AB" }
    val fondo = colorDesde(logo?.colorFondo, colorPrincipal)
    val colorTexto = colorDesde(logo?.colorTexto, Color.White)
    val forma = when (logo?.forma) {
        "CIRCULO" -> CircleShape
        "CUADRADO" -> RectangleShape
        else -> RoundedCornerShape8
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(forma)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = familiaDe(logo?.tipografia.orEmpty()),
            fontWeight = FontWeight.Bold
        )
    }
}

private val RoundedCornerShape8 = RoundedCornerShape(16.dp)

private fun colorDesde(hex: String?, porDefecto: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(porDefecto)

private fun coloresDe(paleta: String): List<Color> =
    Regex("#[0-9A-Fa-f]{6}")
        .findAll(paleta)
        .mapNotNull { runCatching { Color(android.graphics.Color.parseColor(it.value)) }.getOrNull() }
        .distinct()
        .take(5)
        .toList()

private fun familiaDe(nombre: String): FontFamily = when {
    nombre.contains("mono", ignoreCase = true) -> FontFamily.Monospace
    nombre.contains("serif", ignoreCase = true) ||
        nombre.contains("playfair", ignoreCase = true) ||
        nombre.contains("georgia", ignoreCase = true) -> FontFamily.Serif
    else -> FontFamily.SansSerif
}
