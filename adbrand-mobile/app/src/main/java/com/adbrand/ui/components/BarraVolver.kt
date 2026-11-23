package com.adbrand.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import androidx.compose.foundation.background

/**
 * Pantalla que se abre desde "Cuenta" (Perfil de marca, Datos de la empresa): arriba un "← Cuenta" para volver
 * y debajo la pantalla de siempre. statusBarsPadding consume el espacio de la barra del sistema, así la pantalla
 * de abajo no lo vuelve a sumar.
 */
@Composable
fun ConVolver(
    textoVolver: String = "Cuenta",
    alVolver: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClaro)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = alVolver)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = IndigoAdbrand)
                Text(textoVolver, style = MaterialTheme.typography.titleSmall, color = IndigoAdbrand)
            }
        }
        Box(modifier = Modifier.weight(1f)) { contenido() }
    }
}
