package com.adbrand.ui.perfil.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.ui.perfil.viewmodel.DatosEmpresaViewModel

// HU 41: pantalla de datos de la empresa, con validación de RUC y teléfono y cierre de sesión.
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
        topBar = {
            TopAppBar(
                title = { Text("Datos de la empresa") },
                actions = {
                    TextButton(onClick = viewModel::cerrarSesion) {
                        Text("Cerrar sesión")
                    }
                }
            )
        }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = estado.nombreEmpresa.ifBlank { "Mi empresa" },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = estado.correo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (estado.cargando) {
                CircularProgressIndicator()
            }

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

            estado.error?.let { mensaje ->
                Text(mensaje, color = MaterialTheme.colorScheme.error)
            }
            estado.mensaje?.let { mensaje ->
                Text(mensaje, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = viewModel::guardar,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar datos")
                }
            }
        }
    }
}
