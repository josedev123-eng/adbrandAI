package com.adbrand.ui.revision.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbrand.domain.model.ContenidoConEstado
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.revision.viewmodel.EstadosContenidoEstado

// Lo que se puede hacer con cada anuncio de la lista (HU 63).
class AccionesEstados(
    val alBuscar: (String) -> Unit = {},
    val alVerCancelados: (Boolean) -> Unit = {},
    val alVerMas: () -> Unit = {},
    val alCancelar: (Long) -> Unit = {},
    val alReactivar: (Long) -> Unit = {},
    val alEliminar: (Long) -> Unit = {}
)

// HU 56: sección "Estado de tus contenidos", debajo de la lista por revisar. Cada contenido lleva su etiqueta.
// HU 63: buscador por texto o fecha, "Todos" / "Cancelados", de a 12 con "Ver más", "Cancelar en AdBrand",
// "Volver a activar" y "Eliminar" (solo en los cancelados).
fun LazyListScope.seccionEstados(
    estado: EstadosContenidoEstado,
    alReintentar: () -> Unit,
    acciones: AccionesEstados = AccionesEstados()
) {
    item {
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estado de tus contenidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = estado.busqueda,
                onValueChange = acciones.alBuscar,
                singleLine = true,
                label = { Text("Buscar por texto o fecha (AAAA-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !estado.soloCancelados,
                    onClick = { acciones.alVerCancelados(false) },
                    label = { Text("Todos (${estado.cantidadTodos})") }
                )
                FilterChip(
                    selected = estado.soloCancelados,
                    onClick = { acciones.alVerCancelados(true) },
                    label = { Text("Cancelados (${estado.cantidadCancelados})") }
                )
            }
        }
    }

    estado.mensaje?.let { mensaje -> item { AvisoEstado(mensaje) } }

    estado.error?.let { mensaje ->
        item {
            Column {
                AvisoEstado(mensaje, tipo = TipoAviso.ERROR)
                TextButton(onClick = alReintentar) { Text("Reintentar") }
            }
        }
    }

    if (estado.cargado && estado.visibles.isEmpty() && estado.error == null) {
        item {
            AvisoEstado(
                when {
                    estado.busqueda.isNotBlank() -> "No encontramos anuncios con esa búsqueda."
                    estado.soloCancelados -> "No tienes anuncios cancelados."
                    else -> "Todavía no tienes contenidos. Crea tu primer anuncio."
                }
            )
        }
    }

    items(estado.visibles, key = { "estado-${it.id}" }) { contenido ->
        TarjetaEstado(contenido, ocupado = estado.procesandoId == contenido.id, acciones = acciones)
    }

    if (estado.restantes > 0) {
        item {
            OutlinedButton(onClick = acciones.alVerMas, modifier = Modifier.fillMaxWidth()) {
                Text("Ver más (${estado.restantes})")
            }
        }
    }
}

@Composable
private fun TarjetaEstado(contenido: ContenidoConEstado, ocupado: Boolean, acciones: AccionesEstados) {
    var confirmar by remember { mutableStateOf<String?>(null) }
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
                    contenido.titular.ifBlank { contenido.oferta },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                EtiquetaEstado(contenido)
            }
            if (contenido.fecha.isNotBlank()) {
                Text(contenido.fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (contenido.motivo.isNotBlank() && !contenido.cancelado) {
                Text(
                    "Motivo: ${contenido.motivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                contenido.cancelado -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { acciones.alReactivar(contenido.id) }, enabled = !ocupado) { Text("Volver a activar") }
                    TextButton(onClick = { confirmar = "ELIMINAR" }, enabled = !ocupado) {
                        Text("🗑 Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                contenido.estadoActual == "APROBADO" || contenido.estadoActual == "PENDIENTE" -> Column {
                    TextButton(onClick = { confirmar = "CANCELAR" }, enabled = !ocupado) {
                        Text("✕ Cancelar en AdBrand", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Bórralo si ya lo subiste.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Confirmación antes de cancelar o eliminar.
    confirmar?.let { tipo ->
        val esEliminar = tipo == "ELIMINAR"
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text(if (esEliminar) "¿Eliminar este anuncio?" else "¿Cancelar en AdBrand?") },
            text = {
                Text(
                    if (esEliminar) "Ya no lo verás más en tu lista."
                    else "No se publicará y se quitará de tu calendario. Bórralo si ya lo subiste."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = null
                    if (esEliminar) acciones.alEliminar(contenido.id) else acciones.alCancelar(contenido.id)
                }) { Text(if (esEliminar) "Sí, eliminar" else "Sí, cancelar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("No") } }
        )
    }
}

// Verde aprobado, gris cancelado, rojo rechazado por el moderador, ámbar en revisión y gris pendiente.
@Composable
private fun EtiquetaEstado(contenido: ContenidoConEstado) {
    val (fondo, letra) = when (contenido.estadoActual) {
        "APROBADO" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "RECHAZADO" -> Color(0xFFF1F5F9) to Color(0xFF475569)
        "RECHAZADO_MODERADOR" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
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
