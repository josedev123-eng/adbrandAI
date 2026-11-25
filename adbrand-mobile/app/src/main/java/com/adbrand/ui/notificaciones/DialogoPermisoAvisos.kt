package com.adbrand.ui.notificaciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario

// HU 50: invitación amable al permiso de avisos. Explica el beneficio antes de
// que Android muestre su diálogo; si dice "ahora no", no se vuelve a insistir.
@Composable
fun DialogoPermisoAvisos(
    visible: Boolean,
    alActivar: () -> Unit,
    alCerrar: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = alCerrar,
        icon = {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = IndigoAdbrand
            )
        },
        title = {
            Text(
                text = "¿Te avisamos a tiempo?",
                style = MaterialTheme.typography.headlineSmall,
                color = TextoPrincipal
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Activamos los avisos para recordarte cuándo empieza o termina una campaña y cuándo vence tu plan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Text(
                    text = "Puedes cambiarlo cuando quieras en Perfil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = {
            TextButton(onClick = alActivar) {
                Text("Activar avisos", color = IndigoAdbrand)
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) {
                Text("Ahora no", color = TextoSecundario)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = IndigoAdbrand,
        titleContentColor = TextoPrincipal,
        textContentColor = TextoSecundario
    )
}
