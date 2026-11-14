package com.adbrand.ui.revision.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.domain.model.ContenidoRevision
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.revision.viewmodel.RevisionViewModel

// HU 53: lista de contenidos por revisar con el botón Aprobar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    viewModel: RevisionViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Por revisar") }) }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Aprueba con un toque el contenido que quieras publicar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            estado.aviso?.let { mensaje ->
                item {
                    Column {
                        AvisoEstado(mensaje, tipo = TipoAviso.EXITO)
                        TextButton(onClick = viewModel::cerrarAviso) { Text("Entendido") }
                    }
                }
            }

            estado.error?.let { mensaje ->
                item {
                    Column {
                        AvisoEstado(mensaje, tipo = TipoAviso.ERROR)
                        TextButton(onClick = viewModel::cargar) { Text("Reintentar") }
                    }
                }
            }

            if (estado.cargando) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!estado.cargando && estado.cargado && estado.contenidos.isEmpty() && estado.error == null) {
                item {
                    AvisoEstado("No tienes contenido por revisar. Cuando generes anuncios aparecerán aquí.")
                }
            }

            items(estado.contenidos, key = { it.id }) { contenido ->
                TarjetaRevision(
                    contenido = contenido,
                    aprobando = estado.procesandoId == contenido.id,
                    ocupado = estado.procesandoId != null,
                    alAprobar = { viewModel.aprobar(contenido.id) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaRevision(
    contenido: ContenidoRevision,
    aprobando: Boolean,
    ocupado: Boolean,
    alAprobar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    contenido.oferta,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                EtiquetaRed(contenido.redSocial)
            }
            Text(contenido.texto, style = MaterialTheme.typography.bodyMedium)

            when {
                contenido.puedeAprobar -> BotonPrincipal(
                    texto = "Aprobar",
                    onClick = alAprobar,
                    cargando = aprobando,
                    habilitado = !ocupado,
                    icono = Icons.Filled.Check
                )
                // Criterio 2: lo rechazado por el moderador no muestra el botón.
                contenido.estado == "RECHAZADO" -> AvisoEstado(
                    "Rechazado por el moderador" +
                        if (contenido.motivoRevision.isNotBlank()) ": ${contenido.motivoRevision}" else ".",
                    tipo = TipoAviso.ERROR
                )
                else -> AvisoEstado("Un moderador lo está revisando. Podrás aprobarlo cuando termine.")
            }
        }
    }
}

@Composable
private fun EtiquetaRed(redSocial: String) {
    val nombre = when (redSocial) {
        "INSTAGRAM" -> "Instagram"
        "FACEBOOK" -> "Facebook"
        "TIKTOK" -> "TikTok"
        else -> redSocial.lowercase().replaceFirstChar { it.uppercase() }
    }
    Surface(color = Color(0xFFEEF0FF), shape = RoundedCornerShape(50)) {
        Text(
            nombre,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF4F46E5),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
