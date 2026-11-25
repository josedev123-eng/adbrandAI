package com.adbrand.ui.revision.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbrand.domain.model.ContenidoConEstado
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.revision.viewmodel.EstadosContenidoEstado

// HU 56: sección "Estado de tus contenidos", debajo de la lista por revisar.
// Cada contenido lleva una etiqueta de color con su estado actual.
fun LazyListScope.seccionEstados(estado: EstadosContenidoEstado, alReintentar: () -> Unit) {
    item {
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Estado de tus contenidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Lo último que cambió aparece primero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    estado.error?.let { mensaje ->
        item {
            Column {
                AvisoEstado(mensaje, tipo = TipoAviso.ERROR)
                TextButton(onClick = alReintentar) { Text("Reintentar") }
            }
        }
    }

    if (estado.cargado && estado.contenidos.isEmpty() && estado.error == null) {
        item { AvisoEstado("Todavía no tienes contenidos. Genera tu primer anuncio en la web.") }
    }

    items(estado.contenidos, key = { "estado-${it.id}" }) { contenido ->
        TarjetaEstado(contenido)
    }
}

@Composable
private fun TarjetaEstado(contenido: ContenidoConEstado) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    contenido.oferta,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                EtiquetaEstado(contenido)
            }
            if (contenido.motivo.isNotBlank()) {
                Text(
                    "Motivo: ${contenido.motivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Verde aprobado, rojo rechazado, ámbar en revisión y gris pendiente.
@Composable
private fun EtiquetaEstado(contenido: ContenidoConEstado) {
    val (fondo, letra) = when (contenido.estadoActual) {
        "APROBADO" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "RECHAZADO", "RECHAZADO_MODERADOR" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        "EN_REVISION" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }
    Surface(color = fondo, shape = RoundedCornerShape(50)) {
        Text(
            contenido.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = letra,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
