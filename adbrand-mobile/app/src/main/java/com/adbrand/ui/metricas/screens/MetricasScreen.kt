package com.adbrand.ui.metricas.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.domain.model.ContenidoPorMes
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.OpcionPeriodo
import com.adbrand.domain.model.ProximaPublicacion
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.metricas.viewmodel.MetricasViewModel

// Mismos colores de estado que la web: verde aprobado, ámbar en revisión, rojo rechazado.
private val VerdeAprobado = Color(0xFF16733C)
private val AmbarRevision = Color(0xFFA15C07)
private val RojoRechazado = Color(0xFFB91C1C)

private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

// HU 57: tarjetas con los anuncios generados, aprobados, en revisión y rechazados, y la cantidad por mes.
// HU 58: debajo, las publicaciones del calendario por red social.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricasScreen(
    viewModel: MetricasViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Métricas") }) }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelectorPeriodo(
                    opcion = estado.periodo,
                    desde = estado.desdeTexto,
                    hasta = estado.hastaTexto,
                    alElegir = viewModel::elegirPeriodo,
                    alCambiarDesde = viewModel::cambiarDesde,
                    alCambiarHasta = viewModel::cambiarHasta
                )
            }

            if (estado.cargando) {
                item {
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

            if (estado.cargado && estado.error == null) {
                val metricas = estado.metricas
                if (metricas.sinContenido) {
                    item {
                        AvisoEstado("Todavía no generaste anuncios. Crea el primero desde la web para ver tus métricas.")
                    }
                }
                item { TarjetasMetricas(metricas) }
                if (metricas.porMes.isNotEmpty()) {
                    item { AnunciosPorMes(metricas.porMes) }
                }
                item { PublicacionesPorRedSocial(estado.publicaciones) }
                item { ProximasPublicaciones(estado.publicaciones.proximas) }
            }
        }
    }
}

// HU 60: elegir el periodo de las métricas. Con "Rango propio" aparecen los campos Desde y Hasta.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorPeriodo(
    opcion: OpcionPeriodo,
    desde: String,
    hasta: String,
    alElegir: (OpcionPeriodo) -> Unit,
    alCambiarDesde: (String) -> Unit,
    alCambiarHasta: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpcionPeriodo.entries.forEach { posible ->
                FilterChip(
                    selected = posible == opcion,
                    onClick = { alElegir(posible) },
                    label = { Text(posible.etiqueta) }
                )
            }
        }
        if (opcion == OpcionPeriodo.RANGO) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = desde,
                    onValueChange = alCambiarDesde,
                    label = { Text("Desde") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hasta,
                    onValueChange = alCambiarHasta,
                    label = { Text("Hasta") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TarjetasMetricas(metricas: MetricasContenido) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaNumero("Generados", metricas.total, IndigoAdbrand, Modifier.weight(1f))
            TarjetaNumero("Aprobados", metricas.aprobados, VerdeAprobado, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaNumero("En revisión", metricas.enRevision, AmbarRevision, Modifier.weight(1f))
            TarjetaNumero("Rechazados", metricas.rechazados, RojoRechazado, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TarjetaNumero(etiqueta: String, valor: Long, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.testTag("metrica-$etiqueta"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            Text(valor.toString(), style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// Una barra por mes, proporcional al mes con más anuncios.
@Composable
private fun AnunciosPorMes(porMes: List<ContenidoPorMes>) {
    val maximo = porMes.maxOf { it.cantidad }.coerceAtLeast(1)
    TarjetaSeccion(titulo = "Anuncios por mes") {
        porMes.forEach { mes ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(etiquetaMes(mes.mes), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp))
                Box(
                    modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(BordeSuave)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(mes.cantidad.toFloat() / maximo)
                            .clip(RoundedCornerShape(7.dp))
                            .background(IndigoAdbrand)
                    )
                }
                Text(mes.cantidad.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// HU 58: totales del calendario y un gráfico con dos barras por red social (programadas y publicadas).
@Composable
private fun PublicacionesPorRedSocial(publicaciones: MetricasPublicaciones) {
    val maximo = publicaciones.porRed.maxOfOrNull { maxOf(it.programadas, it.publicadas) }?.coerceAtLeast(1) ?: 1
    TarjetaSeccion(
        titulo = "Publicaciones",
        subtitulo = "Las mismas que tienes en el calendario de la web."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaNumero("Programadas", publicaciones.programadas, IndigoAdbrand, Modifier.weight(1f))
            TarjetaNumero("Publicadas", publicaciones.publicadas, VerdeAprobado, Modifier.weight(1f))
            TarjetaNumero("Canceladas", publicaciones.canceladas, TextoSecundario, Modifier.weight(1f))
        }
        publicaciones.porRed.forEach { red ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(nombreRed(red.redSocial), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                BarraConValor("Programadas", red.programadas, maximo, IndigoAdbrand)
                BarraConValor("Publicadas", red.publicadas, maximo, VerdeAprobado)
            }
        }
    }
}

@Composable
private fun BarraConValor(etiqueta: String, valor: Long, maximo: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(84.dp))
        Box(modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(BordeSuave)) {
            if (valor > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(valor.toFloat() / maximo)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }
        }
        Text(valor.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

// HU 58: las siguientes publicaciones programadas, con su fecha, hora, red y campaña.
@Composable
private fun ProximasPublicaciones(proximas: List<ProximaPublicacion>) {
    TarjetaSeccion(titulo = "Próximas publicaciones") {
        if (proximas.isEmpty()) {
            Text(
                "No tienes publicaciones programadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        proximas.forEach { publicacion ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.width(64.dp)) {
                    Text(fechaCorta(publicacion.fecha), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(publicacion.hora.take(5), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(publicacion.titulo, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    Text(nombreRed(publicacion.redSocial), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                publicacion.campanaNombre?.let { nombre ->
                    val color = colorDesdeHex(publicacion.campanaColor) ?: IndigoAdbrand
                    Text(
                        nombre,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = .12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// "2026-11-25" → "25 Nov".
internal fun fechaCorta(fecha: String): String {
    val partes = fecha.split("-")
    val mes = partes.getOrNull(1)?.toIntOrNull() ?: return fecha
    val dia = partes.getOrNull(2)?.toIntOrNull() ?: return fecha
    return "$dia ${MESES.getOrElse(mes - 1) { "" }}".trim()
}

// "#C62828" → el color de la campaña. Si viene mal escrito, null.
internal fun colorDesdeHex(hex: String?): Color? {
    val limpio = hex?.removePrefix("#") ?: return null
    if (limpio.length != 6) return null
    val valor = limpio.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or valor)
}

// "INSTAGRAM" → "Instagram".
internal fun nombreRed(red: String): String = when (red) {
    "INSTAGRAM" -> "Instagram"
    "FACEBOOK" -> "Facebook"
    else -> red
}

// "2026-11" → "Nov 2026".
internal fun etiquetaMes(mes: String): String {
    val partes = mes.split("-")
    val numero = partes.getOrNull(1)?.toIntOrNull() ?: return mes
    return "${MESES.getOrElse(numero - 1) { mes }} ${partes[0]}"
}
