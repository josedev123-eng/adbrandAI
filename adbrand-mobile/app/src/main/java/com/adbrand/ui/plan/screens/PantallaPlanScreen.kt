package com.adbrand.ui.plan.screens

import androidx.compose.runtime.setValue
import com.adbrand.BuildConfig

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.AmbarOscuro
import com.adbrand.core.theme.AmbarSuave
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.ExitoSuave
import com.adbrand.core.theme.ExitoVerde
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.domain.model.MiSuscripcion
import com.adbrand.domain.model.PlanDisponible
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonIcono
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.Esqueleto
import com.adbrand.ui.components.IconoDestacado
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.plan.viewmodel.PlanEstado
import com.adbrand.ui.plan.viewmodel.PlanViewModel

// HU 52: pantalla del plan que se abre desde el aviso de vencimiento.
// Muestra el modo de renovación (automática o manual) y el catálogo de planes.
@Composable
fun PantallaPlanScreen(
    alVolver: () -> Unit = {},
    viewModel: PlanViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val contexto = androidx.compose.ui.platform.LocalContext.current
    // "¿Confirmas el pago?": el plan elegido espera la confirmación antes de abrir el pago.
    var planPorConfirmar by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<PlanDisponible?>(null)
    }
    planPorConfirmar?.let { plan ->
        DialogoConfirmarPago(
            plan = plan,
            alConfirmar = {
                planPorConfirmar = null
                abrirPagoEnLaWeb(contexto)
            },
            alCancelar = { planPorConfirmar = null }
        )
    }

    LaunchedEffect(Unit) { viewModel.cargar() }

    PantallaScroll {
        Spacer(modifier = Modifier.height(14.dp))

        EntradaAnimada(indice = 0) {
            EncabezadoPantalla(
                titulo = "Tu plan",
                subtitulo = "Revisa cuándo vence, cómo se renueva y qué planes tienes disponibles.",
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
                    BotonPrincipal(
                        texto = "Reintentar",
                        onClick = viewModel::cargar,
                        icono = Icons.Filled.Refresh
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (estado.cargando && !estado.cargado) {
            EntradaAnimada(indice = 1) { PlanCargando() }
        } else if (estado.error == null) {
            EntradaAnimada(indice = 1) {
                if (estado.suscripcion.tienePlan) {
                    TarjetaSuscripcion(estado.suscripcion)
                } else {
                    TarjetaSinPlan()
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            EntradaAnimada(indice = 2) {
                Text(
                    text = "Planes disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoPrincipal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            estado.planes.forEachIndexed { indice, plan ->
                EntradaAnimada(indice = 3 + indice) {
                    TarjetaPlan(
                        plan = plan,
                        esActual = plan.nombre.equals(estado.suscripcion.plan, ignoreCase = true),
                        alElegir = { planPorConfirmar = plan }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (estado.planes.isEmpty() && estado.cargado) {
                EntradaAnimada(indice = 3) {
                    AvisoEstado(
                        texto = "No hay planes disponibles en este momento.",
                        tipo = TipoAviso.INFO
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun TarjetaSuscripcion(suscripcion: MiSuscripcion) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        .background(IndigoAdbrand)
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconoDestacado(icono = Icons.Filled.Star)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = suscripcion.plan,
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextoPrincipal,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ChipSuave(
                                    texto = suscripcion.estadoEtiqueta,
                                    colorTexto = ExitoVerde,
                                    colorFondo = ExitoSuave
                                )
                                if (suscripcion.montoLegible.isNotBlank()) {
                                    ChipSuave(
                                        texto = "${suscripcion.montoLegible} / mes",
                                        colorTexto = TextoSecundario
                                    )
                                }
                            }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VENCIMIENTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextoSecundario,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = suscripcion.fechaVencimientoLegible,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextoPrincipal
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ESTADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextoSecundario,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = suscripcion.vencimientoTexto,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (suscripcion.vencePronto) AmbarOscuro else TextoPrincipal
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (suscripcion.renovacionAutomatica) IndigoClaro else AmbarSuave,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (suscripcion.renovacionAutomatica) {
                                Icons.Filled.Autorenew
                            } else {
                                Icons.Filled.Warning
                            },
                            contentDescription = null,
                            tint = if (suscripcion.renovacionAutomatica) IndigoAdbrand else AmbarOscuro,
                            modifier = Modifier.height(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = suscripcion.renovacionTexto,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (suscripcion.renovacionAutomatica) IndigoAdbrand else AmbarOscuro,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = suscripcion.renovacionDetalle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoSecundario
                            )
                        }
                    }
                }
            }
        }

        if (suscripcion.vencePronto) {
            AvisoEstado(
                texto = if (suscripcion.renovacionAutomatica) {
                    "Tu plan se renueva pronto. Verifica que tu método de pago esté vigente."
                } else {
                    "Tu plan vence en ${suscripcion.diasRestantes} días. Renueva para no perder tus campañas."
                },
                tipo = if (suscripcion.renovacionAutomatica) TipoAviso.INFO else TipoAviso.ERROR
            )
        }
    }
}

@Composable
private fun TarjetaSinPlan() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconoDestacado(icono = Icons.Filled.Star)
            Text(
                text = "Todavía no tienes un plan activo",
                style = MaterialTheme.typography.titleMedium,
                color = TextoPrincipal
            )
            Text(
                text = "Elige uno de los planes de abajo para seguir creando contenido con IA.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
    }
}

@Composable
private fun TarjetaPlan(plan: PlanDisponible, esActual: Boolean, alElegir: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(
            width = if (esActual) 1.5.dp else 1.dp,
            color = if (esActual) IndigoAdbrand else BordeSuave
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = plan.precioLegible,
                    style = MaterialTheme.typography.titleMedium,
                    color = IndigoAdbrand,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = plan.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipSuave(texto = plan.tokensLegibles)
                if (esActual) {
                    ChipSuave(
                        texto = "Tu plan actual",
                        colorTexto = ExitoVerde,
                        colorFondo = ExitoSuave
                    )
                }
            }
            if (!esActual) {
                BotonPrincipal(texto = "Elegir ${plan.nombre}", onClick = alElegir)
            }
        }
    }
}

// "¿Confirmas el pago?" antes de ir a pagar: plan, precio, renovación mensual y medio de pago (tarjeta o Yape,
// que se elige en la página segura de la web). Solo con "Sí, pagar" se abre el pago.
@Composable
private fun DialogoConfirmarPago(plan: PlanDisponible, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("¿Confirmas el pago?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Plan: ${plan.nombre}")
                Text("Precio: ${plan.precioLegible}", fontWeight = FontWeight.Bold, color = IndigoAdbrand)
                Text("Medio de pago: tarjeta o Yape (lo eliges en la página segura de pago)")
                Text("Renovación: mensual")
                Text(
                    "Al confirmar se abre la página de pago de AdBrand con Mercado Pago.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = alConfirmar) { Text("Sí, pagar") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

// El pago (tarjeta o Yape con Mercado Pago) se hace en la web, como en Spotify o Canva: se abre la página de planes
// en el navegador. Ahí se inicia sesión con la misma cuenta y se paga; al volver a la app, el plan ya aparece.
private fun abrirPagoEnLaWeb(contexto: android.content.Context) {
    val url = BuildConfig.WEB_URL.trimEnd('/') + "/planes"
    runCatching {
        contexto.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun PlanCargando() {
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
            Esqueleto(modifier = Modifier.fillMaxWidth(0.5f), altura = 26.dp)
            Esqueleto(modifier = Modifier.fillMaxWidth(0.7f))
            Esqueleto(modifier = Modifier.fillMaxWidth())
        }
    }
}
