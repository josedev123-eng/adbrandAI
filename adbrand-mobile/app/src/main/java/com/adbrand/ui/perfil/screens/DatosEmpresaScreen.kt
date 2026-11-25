package com.adbrand.ui.perfil.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.DegradadoMarca
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.BotonSecundario
import com.adbrand.ui.components.CampoAdbrand
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.LogoApp
import com.adbrand.ui.components.PantallaScroll
import com.adbrand.ui.components.TarjetaHero
import com.adbrand.ui.components.TarjetaSeccion
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.perfil.viewmodel.DatosEmpresaViewModel
import kotlinx.coroutines.delay

// HU 41: datos fiscales y de contacto de la empresa, con cierre de sesión.
@Composable
fun DatosEmpresaScreen(
    alCerrarSesion: () -> Unit = {},
    viewModel: DatosEmpresaViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.sesionCerrada) {
        if (estado.sesionCerrada) alCerrarSesion()
    }

    PantallaScroll(espaciado = 16.dp) {
        Spacer(modifier = Modifier.height(14.dp))

        EntradaAnimada(indice = 0) {
            EncabezadoPantalla(
                titulo = "Mi empresa",
                subtitulo = "Tus datos fiscales y de contacto, siempre a mano."
            )
        }

        EntradaAnimada(indice = 1) {
            TarjetaHero(
                etiqueta = "ESPACIO DE MARCA",
                titulo = estado.nombreEmpresa.ifBlank { "Tu empresa" },
                subtitulo = estado.correo.ifBlank { "Cuenta sin correo registrado" },
                iniciales = inicialesDe(estado.nombreEmpresa)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChipSuave(
                        texto = "CUENTA ACTIVA",
                        colorTexto = Blanco,
                        colorFondo = Color.White.copy(alpha = 0.18f)
                    )
                    if (estado.cargando) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Blanco
                            )
                            Text(
                                text = "Actualizando…",
                                style = MaterialTheme.typography.labelSmall,
                                color = Blanco.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        EntradaAnimada(indice = 2) {
            TarjetaSeccion(
                titulo = "Datos de la empresa",
                subtitulo = "Completa esta información para tus comprobantes.",
                icono = Icons.Filled.Business
            ) {
                CampoAdbrand(
                    valor = estado.ruc,
                    alCambiar = viewModel::actualizarRuc,
                    etiqueta = "RUC",
                    icono = Icons.Filled.Numbers,
                    marcador = "20123456789",
                    error = estado.errorRuc,
                    ayuda = "11 dígitos que empiezan en 10 o 20.",
                    tipoTeclado = KeyboardType.Number
                )

                CampoAdbrand(
                    valor = estado.razonSocial,
                    alCambiar = viewModel::actualizarRazonSocial,
                    etiqueta = "Razón social",
                    icono = Icons.Filled.Business,
                    error = estado.errorRazonSocial
                )

                CampoAdbrand(
                    valor = estado.telefono,
                    alCambiar = viewModel::actualizarTelefono,
                    etiqueta = "Teléfono",
                    icono = Icons.Filled.Phone,
                    marcador = "987654321",
                    error = estado.errorTelefono,
                    tipoTeclado = KeyboardType.Phone
                )

                CampoAdbrand(
                    valor = estado.direccion,
                    alCambiar = viewModel::actualizarDireccion,
                    etiqueta = "Dirección",
                    icono = Icons.Filled.Place,
                    marcador = "Av. Siempre Viva 123, Lima",
                    error = estado.errorDireccion,
                    accionIme = ImeAction.Done
                )

                estado.error?.let { AvisoEstado(texto = it, tipo = TipoAviso.ERROR) }
                estado.mensaje?.let { AvisoEstado(texto = it, tipo = TipoAviso.EXITO) }

                BotonPrincipal(
                    texto = "Guardar datos",
                    onClick = viewModel::guardar,
                    cargando = estado.guardando,
                    icono = Icons.Filled.CheckCircle
                )
            }
        }

        EntradaAnimada(indice = 3) {
            TarjetaSeccion(
                titulo = "Sesión",
                subtitulo = "Cierra sesión en este dispositivo cuando termines.",
                icono = Icons.AutoMirrored.Filled.Logout
            ) {
                BotonSecundario(
                    texto = "Cerrar sesión",
                    onClick = viewModel::cerrarSesion,
                    icono = Icons.AutoMirrored.Filled.Logout
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

private fun inicialesDe(nombre: String): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.isEmpty() -> "AB"
        palabras.size == 1 -> palabras.first().take(2).uppercase()
        else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
    }
}
