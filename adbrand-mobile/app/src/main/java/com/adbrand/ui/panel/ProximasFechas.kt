package com.adbrand.ui.panel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.CoralTexto
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.VioletaAdbrand
import com.adbrand.ui.panel.viewmodel.ProximasFechasViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

// La idea elegida en "Próximas fechas" que la pestaña Crear escribe sola al abrirse (se usa una vez).
object OfertaSugerida {
    private var pendiente: String? = null
    fun guardar(idea: String) { pendiente = idea }
    fun tomar(): String? = pendiente.also { pendiente = null }
}

private val FORMATO_DIA = DateTimeFormatter.ofPattern("EEE d 'de' MMM", Locale("es", "PE"))

// Tarjeta "Próximas fechas" del Inicio: las 3 fechas que vienen, y "Crear anuncio" con la idea ya escrita.
@Composable
fun ProximasFechas(alCrearAnuncio: (String) -> Unit, viewModel: ProximasFechasViewModel = viewModel()) {
    val fechas by viewModel.fechas.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }
    if (fechas.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = IndigoAdbrand)
                Text("Próximas fechas que venden", style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
            }
            fechas.forEach { f ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            f.fecha.format(FORMATO_DIA).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = IndigoAdbrand
                        )
                        Text(
                            when (f.diasFaltan) {
                                0L -> "¡Es hoy!"
                                1L -> "Mañana"
                                else -> "Faltan ${f.diasFaltan} días"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = CoralTexto
                        )
                    }
                    Text(f.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                    Text(f.idea, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    TextButton(onClick = { alCrearAnuncio(f.idea) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VioletaAdbrand)
                        Text("  Crear anuncio para esta fecha", color = VioletaAdbrand)
                    }
                }
            }
        }
    }
}
