package com.adbrand.ui.notificaciones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbrand.core.notificaciones.AvisoPush
import com.adbrand.core.notificaciones.AvisosPush
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.IconoDestacado

// HU 50: banner de primer plano. Aparece arriba, se puede tocar para ir a la
// pantalla del aviso y se cierra solo a los pocos segundos.
@Composable
fun BannerAviso(
    aviso: AvisoPush?,
    modifier: Modifier = Modifier,
    alAbrir: (AvisoPush) -> Unit,
    alCerrar: () -> Unit
) {
    AnimatedVisibility(
        visible = aviso != null,
        modifier = modifier,
        enter = slideInVertically(tween(280)) { alto -> -alto } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(220)) { alto -> -alto } + fadeOut(tween(160))
    ) {
        val actual = aviso ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable { alAbrir(actual) },
            shape = RoundedCornerShape(18.dp),
            color = SuperficieBlanca,
            border = BorderStroke(1.dp, BordeSuave),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconoDestacado(
                    icono = Icons.Filled.NotificationsActive,
                    tamano = 42.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ChipSuave(texto = AvisosPush.etiqueta(actual.tipo))
                    Text(
                        text = actual.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrincipal
                    )
                    Text(
                        text = actual.cuerpo,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = alCerrar) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar aviso",
                        tint = TextoSecundario
                    )
                }
            }
        }
    }
}
