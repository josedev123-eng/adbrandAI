package com.adbrand.ui.panel.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.AmbarOscuro
import com.adbrand.core.theme.AmbarSuave
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.ErrorRojo
import com.adbrand.core.theme.ErrorSuave
import com.adbrand.core.theme.ExitoSuave
import com.adbrand.core.theme.ExitoVerde
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.domain.model.CampanaInforme
import com.adbrand.domain.model.InformeCampanas
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.Esqueleto
import com.adbrand.ui.components.EstadoVacio
import com.adbrand.ui.components.PildoraEstado
import com.adbrand.ui.components.PildoraFiltro
import com.adbrand.ui.components.TarjetaHero
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.panel.viewmodel.PanelEstado
import com.adbrand.ui.panel.viewmodel.PanelViewModel
import kotlinx.coroutines.delay

// HU 37: panel de consulta del usuario con sus campañas y el estado de cada una.
@Composable
fun PanelScreen(
    alCrearAnuncio: () -> Unit = {},
    // HU 63: "Crear anuncio para esta fecha" con la idea ya escrita.
    alCrearConIdea: (String) -> Unit = {},
    alAbrirAnuncio: (Long) -> Unit = {},
    viewModel: PanelViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClaro)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                EntradaAnimada(0) {
                    EncabezadoPantalla(
                        titulo = "Panel de consulta",
                        subtitulo = "El estado de tu marca y tus campañas, en un vistazo.",
                        accion = {
                            BotonActualizar(
                                cargando = estado.cargando,
                                alActualizar = viewModel::cargar
                            )
                        }
                    )
                }
            }
        }

        estado.error?.let { mensaje ->
            item {
                EntradaAnimada(1) {
                    AvisoEstado(texto = mensaje, tipo = TipoAviso.ERROR)
                }
            }
        }

        if (estado.informe == null && estado.error == null) {
            item { EntradaAnimada(1) { PanelCargando() } }
        }

        estado.informe?.let { informe ->
            item {
                EntradaAnimada(1) {
                    TarjetaHero(
                        etiqueta = "PANEL DE CONSULTA",
                        titulo = informe.empresa.ifBlank { "Tu negocio" },
                        subtitulo = "Consulta tus campañas, revisa su estado y crea contenido nuevo.",
                        iniciales = inicialesDe(informe.empresa)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipSuave(
                                texto = "${informe.total} " +
                                    (if (informe.total == 1) "campaña" else "campañas"),
                                colorTexto = Blanco,
                                colorFondo = Color.White.copy(alpha = 0.18f)
                            )
                            ChipSuave(
                                texto = "${informe.aprobados} " +
                                    (if (informe.aprobados == 1) "aprobada" else "aprobadas"),
                                colorTexto = Blanco,
                                colorFondo = Color.White.copy(alpha = 0.18f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = alCrearAnuncio)
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Blanco,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Crear un anuncio",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Blanco,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Blanco,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { EntradaAnimada(2) { FilaTotales(informe) } }

            // HU 63: las próximas fechas peruanas que venden.
            item { EntradaAnimada(2) { com.adbrand.ui.panel.ProximasFechas(alCrearAnuncio = alCrearConIdea) } }

            item {
                EntradaAnimada(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tus campañas",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextoPrincipal
                        )
                        ChipSuave(
                            texto = "${informe.total} " +
                                (if (informe.total == 1) "campaña" else "campañas")
                        )
                    }
                }
            }

            item {
                EntradaAnimada(4) {
                    FilaFiltros(estado = estado, alFiltrar = viewModel::filtrar)
                }
            }

            when {
                informe.campanas.isEmpty() -> {
                    item {
                        EntradaAnimada(5) {
                            EstadoVacio(
                                icono = Icons.Filled.Star,
                                titulo = "Todavía no tienes campañas",
                                descripcion = "Genera tu primer anuncio y aparecerá aquí con su estado y su red social."
                            ) {
                                BotonPrincipal(
                                    texto = "Crear anuncio",
                                    onClick = alCrearAnuncio,
                                    icono = Icons.Filled.Add
                                )
                            }
                        }
                    }
                }

                estado.campanasVisibles.isEmpty() -> {
                    item {
                        EntradaAnimada(5) {
                            AvisoEstado(
                                texto = "No hay campañas con ese filtro. Prueba con otro estado.",
                                tipo = TipoAviso.INFO
                            )
                        }
                    }
                }

                else -> {
                    itemsIndexed(
                        items = estado.campanasVisibles,
                        key = { _, campana -> campana.id ?: campana.hashCode().toLong() }
                    ) { indice, campana ->
                        EntradaAnimada(indice = 5 + indice) {
                            TarjetaCampana(campana, alTocar = { campana.id?.let(alAbrirAnuncio) })
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Cabecera: actualizar con giro mientras carga
// ---------------------------------------------------------------------------

@Composable
private fun BotonActualizar(cargando: Boolean, alActualizar: () -> Unit) {
    val infinito = rememberInfiniteTransition(label = "actualizar-panel")
    val giro by infinito.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "giro-panel"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clickable(enabled = !cargando, onClick = alActualizar),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Actualizar panel",
                tint = IndigoAdbrand,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = if (cargando) giro else 0f }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Totales
// ---------------------------------------------------------------------------

@Composable
private fun FilaTotales(informe: InformeCampanas) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        TarjetaTotal(
            etiqueta = "Aprobadas",
            valor = informe.aprobados,
            color = ExitoVerde,
            colorFondo = ExitoSuave,
            modifier = Modifier.weight(1f)
        )
        TarjetaTotal(
            etiqueta = "En revisión",
            valor = informe.dudosos,
            color = AmbarOscuro,
            colorFondo = AmbarSuave,
            modifier = Modifier.weight(1f)
        )
        TarjetaTotal(
            etiqueta = "Rechazadas",
            valor = informe.rechazados,
            color = ErrorRojo,
            colorFondo = ErrorSuave,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaTotal(
    etiqueta: String,
    valor: Int,
    color: Color,
    colorFondo: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colorFondo),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            NumeroAnimado(valor = valor, color = color)
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NumeroAnimado(valor: Int, color: Color) {
    var objetivo by remember { mutableIntStateOf(0) }
    LaunchedEffect(valor) { objetivo = valor }
    val animado by animateIntAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "total-animado"
    )
    Text(
        text = "$animado",
        style = MaterialTheme.typography.headlineMedium,
        color = color,
        fontWeight = FontWeight.ExtraBold
    )
}

// ---------------------------------------------------------------------------
// Filtros y campañas
// ---------------------------------------------------------------------------

@Composable
private fun FilaFiltros(estado: PanelEstado, alFiltrar: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PildoraFiltro("Todas", estado.filtro == null, alElegir = { alFiltrar(null) })
        PildoraFiltro("Aprobadas", estado.filtro == "APROBADO", alElegir = { alFiltrar("APROBADO") })
        PildoraFiltro("En revisión", estado.filtro == "DUDOSO", alElegir = { alFiltrar("DUDOSO") })
        PildoraFiltro("Rechazadas", estado.filtro == "RECHAZADO", alElegir = { alFiltrar("RECHAZADO") })
    }
}

@Composable
private fun TarjetaCampana(campana: CampanaInforme, alTocar: () -> Unit) {
    Surface(
        onClick = alTocar,
        enabled = campana.id != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndigoClaro),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = campana.redSocial.iniciales,
                    style = MaterialTheme.typography.labelLarge,
                    color = IndigoOscuro,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = campana.oferta.ifBlank { "Campaña sin oferta" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    PildoraEstado(campana.estado)
                }
                Text(
                    text = "${campana.redSocial.etiqueta} · ${fechaCorta(campana.fecha)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Carga con esqueletos animados
// ---------------------------------------------------------------------------

@Composable
private fun PanelCargando() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Esqueleto(
                    modifier = Modifier.size(46.dp),
                    altura = 46.dp,
                    forma = RoundedCornerShape(14.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Esqueleto(modifier = Modifier.fillMaxWidth(0.55f), altura = 12.dp)
                    Esqueleto(modifier = Modifier.fillMaxWidth(0.35f), altura = 10.dp)
                }
            }
            Esqueleto(modifier = Modifier.fillMaxWidth(), altura = 12.dp)
            Esqueleto(modifier = Modifier.fillMaxWidth(0.8f), altura = 12.dp)
            Esqueleto(modifier = Modifier.fillMaxWidth(), altura = 42.dp, forma = RoundedCornerShape(14.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

private fun inicialesDe(nombre: String): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.isEmpty() -> "AB"
        palabras.size == 1 -> palabras.first().take(2).uppercase()
        else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
    }
}

// "2026-11-03T10:05:00" -> "03/11/2026 · 10:05"
private fun fechaCorta(fecha: String): String {
    if (fecha.length < 10) return fecha
    val dia = fecha.substring(8, 10)
    val mes = fecha.substring(5, 7)
    val anio = fecha.substring(0, 4)
    val hora = if (fecha.length >= 16 && fecha[10] == 'T') " · ${fecha.substring(11, 16)}" else ""
    return "$dia/$mes/$anio$hora"
}
