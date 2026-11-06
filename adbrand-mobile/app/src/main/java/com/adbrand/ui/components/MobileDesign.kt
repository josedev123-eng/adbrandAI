package com.adbrand.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.CoralAdbrand
import com.adbrand.core.theme.IndigoAdbrand

@Composable
fun LogoApp(modifier: Modifier = Modifier, tamano: Dp = 54.dp, sombra: Boolean = true) {
    Box(
        modifier = modifier.size(tamano).clip(RoundedCornerShape(tamano * .3f))
            .background(Brush.linearGradient(listOf(IndigoAdbrand, Color(0xFF8B5CF6), CoralAdbrand))),
        contentAlignment = Alignment.Center
    ) {
        Text("AB", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun TarjetaSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!subtitulo.isNullOrBlank()) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            contenido()
        }
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    habilitado: Boolean = true,
    icono: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = IndigoAdbrand)
    ) {
        if (cargando) CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = Color.White)
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(texto, fontWeight = FontWeight.Bold)
                if (icono != null) Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

enum class TipoAviso { INFO, EXITO, ERROR }

@Composable
fun AvisoEstado(texto: String, modifier: Modifier = Modifier, tipo: TipoAviso = TipoAviso.INFO) {
    val fondo = when (tipo) {
        TipoAviso.INFO -> MaterialTheme.colorScheme.primaryContainer
        TipoAviso.EXITO -> Color(0xFFE4F6EA)
        TipoAviso.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val tinta = when (tipo) {
        TipoAviso.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
        TipoAviso.EXITO -> Color(0xFF16733C)
        TipoAviso.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(modifier = modifier.fillMaxWidth(), color = fondo, shape = RoundedCornerShape(13.dp)) {
        Text(texto, color = tinta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(13.dp))
    }
}

data class ItemNavegacion(val ruta: String, val etiqueta: String, val icono: ImageVector)

@Composable
fun BarraInferior(rutaActual: String?, items: List<ItemNavegacion>, alNavegar: (String) -> Unit) {
    Surface(color = Color.White, shadowElevation = 13.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                DestinoInferior(item, rutaActual == item.ruta, Modifier.weight(1f)) { alNavegar(item.ruta) }
            }
        }
    }
}

@Composable
private fun RowScope.DestinoInferior(item: ItemNavegacion, seleccionado: Boolean, modifier: Modifier, alTocar: () -> Unit) {
    val color = if (seleccionado) IndigoAdbrand else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.clip(RoundedCornerShape(17.dp)).clickable(onClick = alTocar).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier.size(if (seleccionado) 42.dp else 38.dp).clip(CircleShape)
                .background(if (seleccionado) Color(0xFFECEBFF) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icono, contentDescription = item.etiqueta, tint = color, modifier = Modifier.size(21.dp))
        }
        Text(item.etiqueta, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium)
    }
}
