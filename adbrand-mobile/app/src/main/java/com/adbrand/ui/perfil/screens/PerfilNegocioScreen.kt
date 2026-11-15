package com.adbrand.ui.perfil.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.DegradadoMarca
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.domain.model.Tono
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.CampoAdbrand
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TarjetaHero
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.perfil.viewmodel.PerfilNegocioViewModel
import kotlinx.coroutines.delay

// HU 44: perfil de marca que usa la IA para escribir con la voz del negocio.
@Composable
fun PerfilNegocioScreen(viewModel: PerfilNegocioViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.cargar() }

    PantallaScroll(espaciado = 16.dp) {
        Spacer(modifier = Modifier.height(14.dp))

        EntradaAnimada(indice = 0) {
            EncabezadoPantalla(
                titulo = "Perfil de marca",
                subtitulo = "Cuéntanos quién eres para crear contenido a tu estilo."
            )
        }

        EntradaAnimada(indice = 1) {
            TarjetaHero(
                etiqueta = "IDENTIDAD DE MARCA",
                titulo = estado.nombreComercial.ifBlank { "Tu negocio" },
                subtitulo = estado.rubro.ifBlank { "Cuéntanos a qué te dedicas" },
                iniciales = inicialesMarca(estado.nombreComercial)
            ) {
                if (estado.cargando) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Blanco
                        )
                        Text(
                            text = "Cargando tu perfil…",
                            style = MaterialTheme.typography.labelSmall,
                            color = Blanco.copy(alpha = 0.85f)
                        )
                    }
                } else {
                    ChipSuave(
                        texto = "LISTO PARA CREAR",
                        colorTexto = Blanco,
                        colorFondo = Color.White.copy(alpha = 0.18f)
                    )
                }
            }
        }

        EntradaAnimada(indice = 2) {
            TarjetaSeccion(
                titulo = "Tu negocio",
                subtitulo = "Cada idea se sentirá propia con estos datos.",
                icono = Icons.Filled.Store
            ) {
                CampoAdbrand(
                    valor = estado.nombreComercial,
                    alCambiar = viewModel::actualizarNombreComercial,
                    etiqueta = "Nombre comercial",
                    icono = Icons.Filled.Store,
                    marcador = "Panetones Doña Rosa",
                    error = estado.errorNombre
                )

                CampoAdbrand(
                    valor = estado.rubro,
                    alCambiar = viewModel::actualizarRubro,
                    etiqueta = "Rubro",
                    icono = Icons.Filled.Face,
                    marcador = "Panadería y pastelería",
                    error = estado.errorRubro
                )

                CampoAdbrand(
                    valor = estado.publicoObjetivo,
                    alCambiar = viewModel::actualizarPublicoObjetivo,
                    etiqueta = "Público objetivo",
                    icono = Icons.Filled.Groups,
                    marcador = "Familias y oficinas de Lima que buscan regalos",
                    error = estado.errorPublico,
                    unaLinea = false,
                    lineasMinimas = 2,
                    accionIme = ImeAction.Next
                )

                Text(
                    text = "Tono de la marca",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal
                )
                Tono.entries.chunked(2).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        fila.forEach { tono ->
                            OpcionTono(
                                tono = tono,
                                seleccionado = estado.tono == tono,
                                modifier = Modifier.weight(1f),
                                alElegir = { viewModel.actualizarTono(tono) }
                            )
                        }
                        // Mantiene el ancho cuando la última fila queda impar.
                        if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                CampoAdbrand(
                    valor = estado.descripcion,
                    alCambiar = viewModel::actualizarDescripcion,
                    etiqueta = "Descripción de tu marca",
                    icono = Icons.Filled.Description,
                    marcador = "Qué haces, a quién ayudas y qué te hace distinto.",
                    unaLinea = false,
                    lineasMinimas = 3,
                    accionIme = ImeAction.Next
                )

                CampoAdbrand(
                    valor = estado.correo,
                    alCambiar = {},
                    etiqueta = "Correo de contacto",
                    habilitado = false,
                    ayuda = "Es el correo con el que iniciaste sesión."
                )

                CampoAdbrand(
                    valor = estado.telefono,
                    alCambiar = viewModel::actualizarTelefono,
                    etiqueta = "Teléfono de contacto",
                    icono = Icons.Filled.Phone,
                    marcador = "987654321",
                    error = estado.errorTelefono,
                    tipoTeclado = KeyboardType.Phone,
                    accionIme = ImeAction.Done
                )

                estado.error?.let { AvisoEstado(texto = it, tipo = TipoAviso.ERROR) }
                estado.mensaje?.let { AvisoEstado(texto = it, tipo = TipoAviso.EXITO) }

                BotonPrincipal(
                    texto = "Guardar perfil",
                    onClick = viewModel::guardar,
                    cargando = estado.guardando,
                    icono = Icons.Filled.CheckCircle
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

// ---------------------------------------------------------------------------
// Selector de tono
// ---------------------------------------------------------------------------

@Composable
private fun RowScope.OpcionTono(
    tono: Tono,
    seleccionado: Boolean,
    modifier: Modifier,
    alElegir: () -> Unit
) {
    val fondo by animateColorAsState(
        targetValue = if (seleccionado) IndigoClaro else SuperficieBlanca,
        label = "tono-fondo"
    )
    val borde by animateColorAsState(
        targetValue = if (seleccionado) IndigoAdbrand else BordeSuave,
        label = "tono-borde"
    )

    Surface(
        modifier = modifier.clickable(onClick = alElegir),
        shape = RoundedCornerShape(14.dp),
        color = fondo,
        border = BorderStroke(if (seleccionado) 1.5.dp else 1.dp, borde)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = tono.etiqueta,
                style = MaterialTheme.typography.labelLarge,
                color = if (seleccionado) IndigoOscuro else TextoPrincipal,
                modifier = Modifier.weight(1f)
            )
            if (seleccionado) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(IndigoAdbrand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Blanco,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun inicialesMarca(nombre: String): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.isEmpty() -> "AB"
        palabras.size == 1 -> palabras.first().take(2).uppercase()
        else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
    }
}
