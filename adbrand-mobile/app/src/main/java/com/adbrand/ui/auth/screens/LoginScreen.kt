package com.adbrand.ui.auth.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.ui.auth.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    alAutenticarse: () -> Unit = {},
    irARegistro: () -> Unit = {},
    irARecuperar: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var mostrarClave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(estado.autenticado) { if (estado.autenticado) alAutenticarse() }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroAcceso()
        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Inicia sesión", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Entra al espacio de tu negocio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = estado.correo,
                    onValueChange = viewModel::actualizarCorreo,
                    label = { Text("Correo") },
                    placeholder = { Text("hola@tunegocio.pe") },
                    isError = estado.errorCorreo != null,
                    supportingText = { estado.errorCorreo?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.contrasena,
                    onValueChange = viewModel::actualizarContrasena,
                    label = { Text("Contraseña") },
                    isError = estado.errorContrasena != null,
                    supportingText = { estado.errorContrasena?.let { Text(it) } },
                    visualTransformation = if (mostrarClave) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { mostrarClave = !mostrarClave }) { Text(if (mostrarClave) "Ocultar" else "Ver") } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onDone = { viewModel.iniciarSesion() }),
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                estado.error?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = viewModel::iniciarSesion,
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (estado.cargando) CircularProgressIndicator(modifier = Modifier.size(21.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("Entrar a mi espacio", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = irARecuperar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("¿Olvidaste tu contraseña?")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("¿Primera vez aquí?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = irARegistro) { Text("Crear cuenta", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun HeroAcceso() {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7650C2), MaterialTheme.colorScheme.secondary)))
            .padding(21.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                Text("✦", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
            }
            Text("AdBrand.AI", modifier = Modifier.padding(start = 11.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("PARA TU NEGOCIO", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Dale forma a\ntus ideas.", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Crea y organiza contenido con la personalidad de tu marca.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
