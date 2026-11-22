package com.adbrand.ui.avisos.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.domain.model.CampoPreferencia
import com.adbrand.ui.avisos.viewmodel.PreferenciasViewModel
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonIcono
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.Esqueleto
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.components.TipoAviso

// HU 51: pantalla de avisos. Cada interruptor se guarda al instante y, si falla
// la red, vuelve solo a su estado anterior sin dejar la pantalla bloqueada.
@Composable
fun PreferenciasAvisosScreen(
    alVolver: () -> Unit = {},
    viewModel: PreferenciasViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    PantallaScroll {
        Spacer(modifier = Modifier.height(14.dp))

        EntradaAnimada(indice = 0) {
            EncabezadoPantalla(
                titulo = "Avisos",
                subtitulo = "Elige qué recordatorios quieres recibir en tu teléfono.",
                accion = {
                    BotonIcono(
                        icono = Icons.AutoMirrored.Filled.ArrowBack,
                        descripcion = "Volver",
                        onClick = alVolver
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        estado.error?.let { mensaje ->
            EntradaAnimada(indice = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvisoEstado(texto = mensaje, tipo = TipoAviso.ERROR)
                    if (!estado.cargado) {
                        BotonPrincipal(
                            texto = "Reintentar",
                            onClick = viewModel::reintentar,
                            icono = Icons.Filled.Refresh
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (estado.cargando && !estado.cargado) {
            EntradaAnimada(indice = 1) { AvisosCargando() }
        } else {
            EntradaAnimada(indice = 1) {
                TarjetaSeccion(
                    titulo = "Recordatorios",
                    subtitulo = estado.preferencias.resumen,
                    icono = Icons.Filled.Campaign
                ) {
                    CampoPreferencia.entries.forEach { campo ->
                        FilaPreferencia(
                            campo = campo,
                            activo = estado.preferencias.valor(campo),
                            guardando = estado.guardando,
                            alCambiar = { valor -> viewModel.alternar(campo, valor) }
                        )
                    }

                    estado.mensaje?.let { AvisoEstado(texto = it, tipo = TipoAviso.EXITO) }

                    NotaAvisosSeguridad()
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

@Composable
private fun FilaPreferencia(
    campo: CampoPreferencia,
    activo: Boolean,
    guardando: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !guardando) { alCambiar(!activo) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IndigoClaro),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconoDe(campo),
                contentDescription = null,
                tint = IndigoAdbrand,
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = campo.titulo,
                style = MaterialTheme.typography.titleSmall,
                color = TextoPrincipal,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = campo.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }

        Switch(
            checked = activo,
            onCheckedChange = null,
            enabled = !guardando,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Blanco,
                checkedTrackColor = IndigoAdbrand,
                uncheckedThumbColor = TextoSecundario,
                uncheckedTrackColor = BordeSuave
            )
        )
    }
}

@Composable
private fun NotaAvisosSeguridad() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = IndigoClaro,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = IndigoAdbrand,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Avisos de tu cuenta",
                    style = MaterialTheme.typography.labelLarge,
                    color = IndigoAdbrand,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Los avisos de seguridad y de tu cuenta siempre llegan, aunque apagues estos recordatorios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

@Composable
private fun AvisosCargando() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Esqueleto(modifier = Modifier.fillMaxWidth(0.5f), altura = 22.dp)
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Esqueleto(modifier = Modifier.size(38.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Esqueleto(modifier = Modifier.fillMaxWidth(0.7f))
                        Esqueleto(modifier = Modifier.fillMaxWidth(0.9f), altura = 10.dp)
                    }
                }
            }
        }
    }
}

private fun iconoDe(campo: CampoPreferencia): ImageVector = when (campo) {
    CampoPreferencia.CAMPANAS -> Icons.Filled.Campaign
    CampoPreferencia.SUSCRIPCIONES -> Icons.Filled.Autorenew
    CampoPreferencia.NOVEDADES -> Icons.Filled.AutoAwesome
}
