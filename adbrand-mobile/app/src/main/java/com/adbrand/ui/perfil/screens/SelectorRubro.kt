package com.adbrand.ui.perfil.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoSecundario

// La misma lista de rubros que la web (adbrand-web/src/features/negocio/models/perfilNegocioApi.js),
// así el perfil guardado en la web se ve y se edita igual en la app.
val RUBROS = listOf(
    "Panadería y pastelería",
    "Restaurante y comida",
    "Bodega y minimarket",
    "Ropa y calzado",
    "Belleza y cuidado personal",
    "Salud y bienestar",
    "Tecnología",
    "Educación",
    "Servicios profesionales",
    "Otro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorRubro(valor: String, alCambiar: (String) -> Unit, error: String?) {
    var abierto by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Rubro") },
            placeholder = { Text("Elige tu rubro") },
            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null, tint = TextoSecundario) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IndigoAdbrand,
                unfocusedBorderColor = BordeSuave,
                focusedContainerColor = SuperficieBlanca,
                unfocusedContainerColor = SuperficieBlanca
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            // Si en la base hay un rubro que no está en la lista (de antes), se muestra igual para no perderlo.
            val opciones = if (valor.isNotBlank() && valor !in RUBROS) listOf(valor) + RUBROS else RUBROS
            opciones.forEach { rubro ->
                DropdownMenuItem(
                    text = { Text(rubro) },
                    onClick = {
                        alCambiar(rubro)
                        abierto = false
                    }
                )
            }
        }
    }
}
