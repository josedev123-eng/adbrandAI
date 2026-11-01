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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.domain.model.Tono
import com.adbrand.ui.perfil.viewmodel.PerfilNegocioViewModel

// HU 44: perfil del negocio (rubro, tono y datos de contacto) con validación antes de guardar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilNegocioScreen(
    viewModel: PerfilNegocioViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Perfil del negocio") }) }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (estado.cargando) {
                CircularProgressIndicator()
            }

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

            Text("Tono de la marca", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Tono.entries.chunked(2).forEach { fila ->
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        fila.forEach { tono ->
                            FilterChip(
                                selected = estado.tono == tono,
                                onClick = { viewModel.actualizarTono(tono) },
                                label = { Text(tono.etiqueta) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = viewModel::actualizarDescripcion,
                label = { Text("Descripción") },
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
                    Text("Guardar perfil")
                }
            }
        }
    }
}
