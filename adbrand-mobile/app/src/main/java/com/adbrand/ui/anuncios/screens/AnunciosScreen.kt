package com.adbrand.ui.anuncios.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.previsualizacion.screens.PrevisualizacionScreen
import com.adbrand.ui.revision.screens.RevisionScreen

// Pestaña "Anuncios": junta lo que antes eran dos pestañas.
// - Por aprobar (HU 53 a 56): la IA propone y la empresa decide qué se publica, como los borradores de Buffer o Later.
// - Vista previa (HU 46 a 48): cómo se ve cada anuncio en cuadrado, vertical u horizontal, para guardarlo o compartirlo.
// "abrirId" abre directo un anuncio en la vista previa (al tocarlo en el panel de inicio).
@Composable
fun AnunciosScreen(
    abrirId: Long? = null,
    alCrearAnuncio: () -> Unit = {},
    alCrearKit: () -> Unit = {}
) {
    val secciones = listOf("Por revisar", "Listos para publicar")
    var elegida by rememberSaveable { mutableIntStateOf(if (abrirId != null) 1 else 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(14.dp))
            EncabezadoPantalla(
                titulo = "Anuncios",
                subtitulo = if (elegida == 0) {
                    "La IA creó estos anuncios. Quédate con los que te gusten."
                } else {
                    "Toca un anuncio, elige dónde lo usarás y compártelo."
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                secciones.forEachIndexed { indice, nombre ->
                    SegmentedButton(
                        selected = elegida == indice,
                        onClick = { elegida = indice },
                        shape = SegmentedButtonDefaults.itemShape(index = indice, count = secciones.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = IndigoAdbrand,
                            activeContentColor = Blanco,
                            inactiveContainerColor = SuperficieBlanca,
                            inactiveContentColor = TextoSecundario,
                            activeBorderColor = IndigoAdbrand,
                            inactiveBorderColor = BordeSuave
                        ),
                        icon = {}
                    ) {
                        Text(nombre, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            when (elegida) {
                0 -> RevisionScreen(conBarraSuperior = false)
                else -> PrevisualizacionScreen(
                    alCrearAnuncio = alCrearAnuncio,
                    conEncabezado = false,
                    abrirId = abrirId,
                    alCrearKit = alCrearKit
                )
            }
        }
    }
}
