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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import com.adbrand.core.theme.CoralAdbrand
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.LogoApp
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.perfil.viewmodel.DatosEmpresaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosEmpresaScreen(
    alCerrarSesion: () -> Unit = {},
    viewModel: DatosEmpresaViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.sesionCerrada) {
        if (estado.sesionCerrada) alCerrarSesion()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mi espacio") },
                actions = {
                    TextButton(onClick = viewModel::cerrarSesion) { Text("Salir") }
                }
            )
        }
    ) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(IndigoAdbrand, Color(0xFF8B5CF6), CoralAdbrand)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        LogoApp(tamano = 54.dp, sombra = false)
                        Column {
                            Text("ESPACIO DE MARCA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f), fontWeight = FontWeight.Bold)
                            Text(estado.nombreEmpresa.ifBlank { "Tu empresa" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(estado.correo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .86f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = .18f)).padding(horizontal = 11.dp, vertical = 6.dp)) {
                            Text("CUENTA ACTIVA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                        if (estado.cargando) Text("Actualizando…", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            TarjetaSeccion(
                titulo = "Datos de la empresa",
                subtitulo = "Completa esta información para personalizar tus herramientas."
            ) {
                OutlinedTextField(
                    value = estado.ruc,
                    onValueChange = viewModel::actualizarRuc,
                    label = { Text("RUC") },
                    isError = estado.errorRuc != null,
                    supportingText = { estado.errorRuc?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.razonSocial,
                    onValueChange = viewModel::actualizarRazonSocial,
                    label = { Text("Razón social") },
                    isError = estado.errorRazonSocial != null,
                    supportingText = { estado.errorRazonSocial?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.telefono,
                    onValueChange = viewModel::actualizarTelefono,
                    label = { Text("Teléfono") },
                    isError = estado.errorTelefono != null,
                    supportingText = { estado.errorTelefono?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.direccion,
                    onValueChange = viewModel::actualizarDireccion,
                    label = { Text("Dirección") },
                    isError = estado.errorDireccion != null,
                    supportingText = { estado.errorDireccion?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                estado.error?.let { AvisoEstado(it, tipo = com.adbrand.ui.components.TipoAviso.ERROR) }
                estado.mensaje?.let { AvisoEstado(it, tipo = com.adbrand.ui.components.TipoAviso.EXITO) }
                BotonPrincipal(
                    texto = "Guardar datos",
                    onClick = viewModel::guardar,
                    cargando = estado.guardando,
                    icono = Icons.Filled.CheckCircle
                )
            }
        }
    }
}
