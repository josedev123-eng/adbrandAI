package com.adbrand.ui.campana.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.domain.model.Campana
import com.adbrand.ui.campana.viewmodel.DetalleCampanaEstado
import com.adbrand.ui.campana.viewmodel.DetalleCampanaViewModel
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonIcono
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.BotonSecundario
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.Esqueleto
import com.adbrand.ui.components.EstadoVacio
import com.adbrand.ui.components.IconoDestacado
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TipoAviso

// HU 49: ficha de la campaña que se abre al tocar el aviso de recordatorio.
// Si la campaña ya no existe (o es de otra empresa) se muestra un estado claro
// con la opción de volver, sin dejar la pantalla en blanco.
@Composable
fun DetalleCampanaScreen(
    id: Long,
    alVolver: () -> Unit = {},
    viewModel: DetalleCampanaViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(id) { viewModel.cargar(id) }

    PantallaScroll {
        Spacer(modifier = Modifier.height(14.dp))

        EntradaAnimada(indice = 0) {
            EncabezadoPantalla(
                titulo = "Detalle de campaña",
                subtitulo = "El aviso te trajo aquí para que no pierdas su inicio ni su cierre.",
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

        when {
            estado.cargando -> EntradaAnimada(indice = 1) { CampanaCargando() }

            estado.campana != null -> EntradaAnimada(indice = 1) {
                FichaCampana(estado.campana!!)
            }

            estado.noEncontrada -> EntradaAnimada(indice = 1) {
                EstadoVacio(
                    icono = Icons.Filled.Search,
                    titulo = "Esta campaña ya no está disponible",
                    descripcion = estado.error
                        ?: "Puede que la hayan eliminado o que sea de otra empresa."
                ) {
                    BotonPrincipal(
                        texto = "Volver al panel",
                        onClick = alVolver
                    )
                }
            }

            estado.error != null -> EntradaAnimada(indice = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvisoEstado(texto = estado.error!!, tipo = TipoAviso.ERROR)
                    BotonPrincipal(
                        texto = "Reintentar",
                        onClick = viewModel::reintentar,
                        icono = Icons.Filled.Refresh
                    )
                    BotonSecundario(
                        texto = "Volver",
                        onClick = alVolver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun FichaCampana(campana: Campana) {
    val colorCampana = recordarColor(campana.color)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = SuperficieBlanca,
            border = BorderStroke(1.dp, BordeSuave),
            shadowElevation = 0.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(colorCampana)
                )

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconoDestacado(
                            icono = Icons.Filled.Event,
                            colorFondo = colorCampana.copy(alpha = 0.16f),
                            colorIcono = colorCampana
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipSuave(
                                texto = if (campana.esEvento) "Evento especial" else "Campaña",
                                colorTexto = IndigoAdbrand
                            )
                            Text(
                                text = campana.nombre.ifBlank { "Campaña sin nombre" },
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextoPrincipal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipSuave(texto = campana.situacion)
                        campana.duracionDias?.let { dias ->
                            ChipSuave(
                                texto = if (dias == 1L) "1 día" else "$dias días",
                                colorTexto = TextoSecundario
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BordeSuave)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DatoFecha(
                            etiqueta = "Empieza",
                            valor = campana.fechaInicioLegible,
                            modifier = Modifier.weight(1f)
                        )
                        DatoFecha(
                            etiqueta = "Termina",
                            valor = campana.fechaFinLegible,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colorCampana.copy(alpha = 0.10f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = colorCampana,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Te avisamos porque esta campaña empieza o termina mañana. " +
                        "Prepara el contenido con tiempo para no perder la fecha.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

@Composable
private fun DatoFecha(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = etiqueta.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextoSecundario,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = valor.ifBlank { "Por confirmar" },
            style = MaterialTheme.typography.bodyLarge,
            color = TextoPrincipal
        )
    }
}

@Composable
private fun CampanaCargando() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Esqueleto(modifier = Modifier.fillMaxWidth(0.55f), altura = 26.dp)
            Esqueleto(modifier = Modifier.fillMaxWidth(0.35f))
            Spacer(modifier = Modifier.height(6.dp))
            Esqueleto(modifier = Modifier.fillMaxWidth())
            Esqueleto(modifier = Modifier.fillMaxWidth(0.8f))
        }
    }
}

@Composable
private fun recordarColor(valor: String): Color = androidx.compose.runtime.remember(valor) {
    runCatching { Color(android.graphics.Color.parseColor(valor)) }.getOrDefault(IndigoAdbrand)
}
