package com.adbrand.ui.auth.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.ui.auth.viewmodel.AccesoCuentaViewModel
import kotlinx.coroutines.delay

@Composable
fun BienvenidaScreen(alContinuar: () -> Unit) {
    LaunchedEffect(Unit) { delay(1500); alContinuar() }
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7650C2), MaterialTheme.colorScheme.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = Color.White.copy(alpha = .17f), shape = RoundedCornerShape(26.dp)) {
                Text("AB", modifier = Modifier.padding(horizontal = 22.dp, vertical = 17.dp), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }
            Text("Bienvenido a", color = Color.White.copy(alpha = .82f), style = MaterialTheme.typography.titleMedium)
            Text("AdBrand.AI", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp), color = Color.White, strokeWidth = 2.dp)
            Text("Preparando tu espacio…", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RegistroScreen(alVolver: () -> Unit, viewModel: AccesoCuentaViewModel = viewModel()) {
    val s by viewModel.estado.collectAsState()
    MarcoAuth("EMPIEZA TU MARCA", "Crea tu cuenta", "Un espacio para organizar tu negocio y crear con identidad.", alVolver) {
        if (!s.terminado) {
            Campo("Tu nombre", s.nombre, viewModel::nombre)
            Campo("Nombre de tu empresa", s.empresa, viewModel::empresa)
            Campo("Correo", s.correo, viewModel::correo, KeyboardType.Email)
            Campo("Contraseña", s.contrasena, viewModel::contrasena, KeyboardType.Password, true)
            Campo("Repite la contraseña", s.repetir, viewModel::repetir, KeyboardType.Password, true)
        }
        s.error?.let { Mensaje(it, true) }
        s.mensaje?.let { Mensaje(it) }
        if (s.terminado) {
            Button(onClick = alVolver, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Ir a iniciar sesión") }
        } else {
            Button(onClick = viewModel::registrar, enabled = !s.cargando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                if (s.cargando) CircularProgressIndicator() else Text("Crear cuenta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecuperarContrasenaScreen(alVolver: () -> Unit, viewModel: AccesoCuentaViewModel = viewModel()) {
    val s by viewModel.estado.collectAsState()
    val titulo = if (s.terminado) "Contraseña actualizada" else "Recuperar contraseña"
    val ayuda = if (s.paso == 1) "Te enviaremos un código de seis dígitos para confirmar tu cuenta." else "Escribe el código y elige una contraseña nueva."
    MarcoAuth("RECUPERA EL ACCESO", titulo, ayuda, alVolver) {
        if (!s.terminado) {
            Campo("Correo de tu cuenta", s.correo, viewModel::correo, KeyboardType.Email)
            if (s.paso == 2) {
                Campo("Código de seis dígitos", s.codigo, viewModel::codigo, KeyboardType.Number)
                Campo("Contraseña nueva", s.nueva, viewModel::nueva, KeyboardType.Password, true)
                Campo("Repite la contraseña", s.repetirNueva, viewModel::repetirNueva, KeyboardType.Password, true)
            }
        }
        s.error?.let { Mensaje(it, true) }
        s.mensaje?.let { Mensaje(it) }
        when {
            s.terminado -> Button(onClick = alVolver, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Volver a iniciar sesión") }
            s.paso == 1 -> Button(onClick = viewModel::pedirCodigo, enabled = !s.cargando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                if (s.cargando) CircularProgressIndicator() else Text("Enviar código", fontWeight = FontWeight.Bold)
            }
            else -> {
                Button(onClick = viewModel::cambiarContrasena, enabled = !s.cargando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    if (s.cargando) CircularProgressIndicator() else Text("Guardar contraseña nueva", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = viewModel::otroCodigo, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Pedir otro código") }
            }
        }
    }
}

@Composable
private fun MarcoAuth(etiqueta: String, titulo: String, subtitulo: String, alVolver: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("AdBrand.AI", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Text(etiqueta, modifier = Modifier.padding(top = 22.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Text(titulo, modifier = Modifier.padding(top = 7.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitulo, modifier = Modifier.padding(top = 7.dp, bottom = 17.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = contenido)
        }
        TextButton(onClick = alVolver, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)) { Text("Volver a iniciar sesión") }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, alCambiar: (String) -> Unit, tipo: KeyboardType = KeyboardType.Text, secreto: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        visualTransformation = if (secreto) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = tipo, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Mensaje(texto: String, error: Boolean = false) {
    val fondo = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(color = fondo, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(texto, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    }
}
