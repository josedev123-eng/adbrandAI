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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TopAppBarDefaults
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
import com.adbrand.ui.revision.viewmodel.MAXIMO_MOTIVO_RECHAZO
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.adbrand.ui.revision.viewmodel.EstadosContenidoViewModel
import com.adbrand.ui.revision.viewmodel.RevisionViewModel

// HU 53 y 54: lista de contenidos por revisar con los botones Aprobar y Rechazar.
// HU 56: debajo, el estado actual de cada contenido con una etiqueta de color.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    // Dentro de la pestaña "Anuncios" el título ya lo pone esa pantalla.
    conBarraSuperior: Boolean = true,
    viewModel: RevisionViewModel = viewModel(),
    estadosViewModel: EstadosContenidoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val estados by estadosViewModel.estado.collectAsState()

    // HU 56: se actualiza al abrir la pestaña y cada vez que se vuelve a ella o a la app.
    LifecycleResumeEffect(Unit) {
        viewModel.cargar()
        estadosViewModel.cargar()
        onPauseOrDispose { }
    }

    // HU 56, criterio 1: después de aprobar o rechazar, la sección muestra el estado nuevo
    // sin cerrar la app (Jose muestra un aviso y saca el contenido de la lista por revisar).
    LaunchedEffect(estado.aviso, estado.contenidos.size) {
        if (estado.aviso != null) estadosViewModel.cargar()
    }

    Scaffold(
        topBar = { if (conBarraSuperior) TopAppBar(
            title = { Text("Por aprobar", style = MaterialTheme.typography.headlineMedium) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        ) }
    ) { relleno ->
        // HU 56, criterio 2: al deslizar la lista hacia abajo se actualizan las dos listas.
        PullToRefreshBox(
            isRefreshing = estados.cargando,
            onRefresh = {
                viewModel.cargar()
                estadosViewModel.cargar()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En dos líneas: qué hace cada botón.
            item {
                AvisoEstado(
                    "\"Me gusta, usar\" lo deja listo para publicar. \"Descartar\" lo quita y no se publica."
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
                    AvisoEstado("No tienes anuncios por revisar. Cuando crees uno con \"+ Crear\", aparecerá aquí.")
                }
            }

            items(estado.contenidos, key = { it.id }) { contenido ->
                TarjetaRevision(
                    contenido = contenido,
                    aprobando = estado.procesandoId == contenido.id,
                    ocupado = estado.procesandoId != null,
                    alAprobar = { viewModel.aprobar(contenido.id) },
                    alRechazar = { viewModel.pedirRechazo(contenido.id) }
                )
            }

            seccionEstados(
                estados,
                alReintentar = estadosViewModel::cargar,
                acciones = AccionesEstados(
                    alBuscar = estadosViewModel::buscar,
                    alVerCancelados = estadosViewModel::verCancelados,
                    alVerMas = estadosViewModel::verMas,
                    alCancelar = estadosViewModel::cancelar,
                    alReactivar = estadosViewModel::reactivar,
                    alEliminar = estadosViewModel::eliminar
                )
            )
        }
        }
    }

    // HU 54: ventanita para confirmar el rechazo con un motivo opcional.
    estado.porRechazar?.let { contenido ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarRechazo,
            title = { Text("¿Descartar este anuncio?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "No se publicará ni se podrá programar en el calendario.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = estado.motivoRechazo,
                        onValueChange = viewModel::cambiarMotivo,
                        label = { Text("¿Por qué? (opcional)") },
                        placeholder = { Text("Ej.: el precio está mal") },
                        supportingText = { Text("${estado.motivoRechazo.length}/$MAXIMO_MOTIVO_RECHAZO") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarRechazo) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarRechazo) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaRevision(
    contenido: ContenidoRevision,
    aprobando: Boolean,
    ocupado: Boolean,
    alAprobar: () -> Unit,
    alRechazar: () -> Unit
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
                    texto = "Me gusta, usar",
                    onClick = alAprobar,
                    cargando = aprobando,
                    habilitado = !ocupado,
                    icono = Icons.Filled.Check
                )
                // Criterio 2: lo rechazado por el moderador no muestra el botón.
                contenido.estado == "RECHAZADO" -> AvisoEstado(
                    "No se puede usar: lo rechazó el equipo de AdBrand.AI" +
                        if (contenido.motivoRevision.isNotBlank()) ": ${contenido.motivoRevision}" else ".",
                    tipo = TipoAviso.ERROR
                )
                else -> AvisoEstado("El equipo de AdBrand.AI lo está revisando. Podrás usarlo cuando termine.")
            }

            // HU 54: cualquier contenido de la lista se puede rechazar (también lo que aún revisa el moderador).
            OutlinedButton(
                onClick = alRechazar,
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Descartar")
            }
        }
    }
}

@Composable
private fun EtiquetaRed(redSocial: String) {
    val nombre = when (redSocial) {
        "INSTAGRAM" -> "Instagram"
        "FACEBOOK" -> "Facebook"
        "WHATSAPP" -> "WhatsApp"
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
