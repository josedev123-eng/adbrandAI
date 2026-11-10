package com.adbrand.ui.perfil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.CoralAdbrand
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.domain.model.Tono
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.LogoApp
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.perfil.viewmodel.PerfilNegocioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilNegocioScreen(viewModel: PerfilNegocioViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Perfil de marca") }) }
    ) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(IndigoAdbrand, Color(0xFF7853C7), CoralAdbrand)))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LogoApp(tamano = 54.dp, sombra = false)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("IDENTIDAD DE MARCA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f), fontWeight = FontWeight.Bold)
                        Text("Cuéntanos quién eres", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        Text("Así crearemos contenido a tu estilo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f))
                    }
                }
            }

            TarjetaSeccion(
                titulo = "Tu negocio",
                subtitulo = "Describe tu marca para que cada idea se sienta propia."
            ) {
                if (estado.cargando) Text("Cargando perfil…", color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = estado.nombreComercial,
                    onValueChange = viewModel::actualizarNombreComercial,
                    label = { Text("Nombre comercial") },
                    isError = estado.errorNombre != null,
                    supportingText = { estado.errorNombre?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.rubro,
                    onValueChange = viewModel::actualizarRubro,
                    label = { Text("Rubro") },
                    isError = estado.errorRubro != null,
                    supportingText = { estado.errorRubro?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.publicoObjetivo,
                    onValueChange = viewModel::actualizarPublicoObjetivo,
                    label = { Text("Público objetivo") },
                    isError = estado.errorPublico != null,
                    supportingText = { estado.errorPublico?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tono de la marca", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Tono.entries.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        fila.forEach { tono ->
                            FilterChip(
                                selected = estado.tono == tono,
                                onClick = { viewModel.actualizarTono(tono) },
                                label = { Text(tono.etiqueta) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = estado.descripcion,
                    onValueChange = viewModel::actualizarDescripcion,
                    label = { Text("Descripción de tu marca") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.correo,
                    onValueChange = {},
                    label = { Text("Correo de contacto") },
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.telefono,
                    onValueChange = viewModel::actualizarTelefono,
                    label = { Text("Teléfono de contacto") },
                    isError = estado.errorTelefono != null,
                    supportingText = { estado.errorTelefono?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                estado.error?.let { AvisoEstado(it, tipo = TipoAviso.ERROR) }
                estado.mensaje?.let { AvisoEstado(it, tipo = TipoAviso.EXITO) }
                BotonPrincipal("Guardar perfil", viewModel::guardar, cargando = estado.guardando, icono = Icons.Filled.CheckCircle)
            }
        }
    }
}
