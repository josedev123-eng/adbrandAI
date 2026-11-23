package com.adbrand.ui.cuenta.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.ErrorRojo
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.TintaSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.cuenta.viewmodel.CuentaViewModel

// Pestaña "Cuenta": todo lo de la empresa en un solo lugar (como "Perfil" o "Ajustes" en Canva o Buffer).
// "Cerrar sesión" está solo aquí, al final, separado del resto para no tocarlo sin querer.
@Composable
fun CuentaScreen(
    alAbrirPerfilMarca: () -> Unit,
    alAbrirKit: () -> Unit,
    alAbrirDatosEmpresa: () -> Unit,
    alAbrirPlan: () -> Unit,
    alAbrirAvisos: () -> Unit,
    alCerrarSesion: () -> Unit,
    alVerGuia: () -> Unit = {},
    viewModel: CuentaViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.sesionCerrada) { if (estado.sesionCerrada) alCerrarSesion() }

    PantallaScroll(espaciado = 14.dp) {
        Spacer(modifier = Modifier.height(14.dp))
        EncabezadoPantalla(titulo = "Cuenta", subtitulo = "Tu marca, tu empresa y tus preferencias.")

        // Quién inició sesión.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SuperficieBlanca,
            border = BorderStroke(1.dp, BordeSuave)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(IndigoAdbrand),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iniciales(estado.nombre),
                        color = Blanco,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = estado.nombre.ifBlank { "Tu empresa" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (estado.correo.isNotBlank()) {
                        Text(
                            text = estado.correo,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        GrupoOpciones {
            OpcionCuenta(Icons.Filled.Storefront, "Perfil de marca", "Rubro, público y tono que usa la IA", alAbrirPerfilMarca)
            Separador()
            OpcionCuenta(Icons.Filled.Palette, "Kit de marca", "Tu logo, tus colores y tus letras", alAbrirKit)
            Separador()
            OpcionCuenta(Icons.Filled.Business, "Datos de la empresa", "RUC, razón social y dirección", alAbrirDatosEmpresa)
            Separador()
            OpcionCuenta(Icons.Filled.WorkspacePremium, "Mi plan", "Tu plan y su vencimiento", alAbrirPlan)
            Separador()
            OpcionCuenta(Icons.Filled.Notifications, "Avisos", "Qué recordatorios recibes", alAbrirAvisos)
            Separador()
            OpcionCuenta(Icons.Filled.SmartToy, "Ver la guía otra vez", "Bran te explica la app en 1 minuto", alVerGuia)
        }

        GrupoOpciones {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !estado.cerrando, onClick = viewModel::cerrarSesion)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRojo)
                Text(
                    text = if (estado.cerrando) "Cerrando sesión…" else "Cerrar sesión",
                    style = MaterialTheme.typography.titleSmall,
                    color = ErrorRojo
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

@Composable
private fun GrupoOpciones(contenido: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column { contenido() }
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(modifier = Modifier.padding(start = 66.dp), color = BordeSuave)
}

@Composable
private fun OpcionCuenta(icono: ImageVector, titulo: String, detalle: String, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IndigoClaro),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = IndigoAdbrand, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TintaSuave)
    }
}

private fun iniciales(nombre: String): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.isEmpty() -> "AB"
        palabras.size == 1 -> palabras.first().take(2).uppercase()
        else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
    }
}
