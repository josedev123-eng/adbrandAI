package com.adbrand.ui.metricas.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.adbrand.domain.model.ContenidoPorMes
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.metricas.viewmodel.MetricasViewModel

// Mismos colores de estado que la web: verde aprobado, ámbar en revisión, rojo rechazado.
private val VerdeAprobado = Color(0xFF16733C)
private val AmbarRevision = Color(0xFFA15C07)
private val RojoRechazado = Color(0xFFB91C1C)

private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

// HU 57: tarjetas con los anuncios generados, aprobados, en revisión y rechazados, y la cantidad por mes.
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

// "2026-11" → "Nov 2026".
internal fun etiquetaMes(mes: String): String {
    val partes = mes.split("-")
    val numero = partes.getOrNull(1)?.toIntOrNull() ?: return mes
    return "${MESES.getOrElse(numero - 1) { mes }} ${partes[0]}"
}
