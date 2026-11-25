package com.adbrand.ui.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.DegradadoMarca
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.PapelHueso
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.VioletaAdbrand
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.CampoAdbrand
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.HaloMarca
import com.adbrand.ui.components.LogoApp
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.auth.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

// HU 42: inicio de sesión con animaciones de entrada y fondo vivo.
@Composable
fun LoginScreen(
    alAutenticarse: () -> Unit = {},
    irARegistro: () -> Unit = {},
    irARecuperar: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var mostrarClave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(estado.autenticado) { if (estado.autenticado) alAutenticarse() }

    FondoAcceso {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            EntradaAnimada(indice = 0) { MarcaAcceso() }

            Spacer(modifier = Modifier.height(26.dp))

            EntradaAnimada(indice = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Bienvenido\nde vuelta.",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextoPrincipal
                    )
                    Text(
                        text = "Entra al espacio de tu negocio y sigue creando con la voz de tu marca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            EntradaAnimada(indice = 3) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SuperficieBlanca,
                    border = BorderStroke(1.dp, BordeSuave)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Inicia sesión",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextoPrincipal
                        )

                        CampoAdbrand(
                            valor = estado.correo,
                            alCambiar = viewModel::actualizarCorreo,
                            etiqueta = "Correo",
                            icono = Icons.Filled.Email,
                            marcador = "hola@tunegocio.pe",
                            error = estado.errorCorreo,
                            tipoTeclado = KeyboardType.Email,
                            accionIme = ImeAction.Next
                        )

                        CampoAdbrand(
                            valor = estado.contrasena,
                            alCambiar = viewModel::actualizarContrasena,
                            etiqueta = "Contraseña",
                            icono = Icons.Filled.Lock,
                            error = estado.errorContrasena,
                            tipoTeclado = KeyboardType.Password,
                            accionIme = ImeAction.Done,
                            transformacion = if (mostrarClave) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailing = {
                                Icon(
                                    imageVector = if (mostrarClave) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (mostrarClave) "Ocultar" else "Ver",
                                    tint = TextoSecundario,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { mostrarClave = !mostrarClave }
                                        .padding(2.dp)
                                        .size(22.dp)
                                )
                            }
                        )

                        estado.error?.let { AvisoEstado(texto = it, tipo = TipoAviso.ERROR) }

                        BotonPrincipal(
                            texto = "Entrar a mi espacio",
                            onClick = viewModel::iniciarSesion,
                            cargando = estado.cargando,
                            icono = Icons.Filled.ArrowForward
                        )

                        TextButton(
                            onClick = irARecuperar,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                style = MaterialTheme.typography.labelLarge,
                                color = IndigoOscuro
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            EntradaAnimada(indice = 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Primera vez aquí?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                    TextButton(onClick = irARegistro) {
                        Text(
                            text = "Crear cuenta",
                            style = MaterialTheme.typography.labelLarge,
                            color = IndigoAdbrand,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Fondo animado
// ---------------------------------------------------------------------------

@Composable
private fun FondoAcceso(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PapelHueso, FondoClaro, Color(0xFFEFEADD))
                )
            )
    ) {
        // El inset de las barras vive fuera del scroll: el contenido nunca se
        // dibuja debajo de la barra de estado ni del teclado.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .clipToBounds()
        ) {
            contenido()
        }
    }
}

// ---------------------------------------------------------------------------
// Marca
// ---------------------------------------------------------------------------

@Composable
private fun MarcaAcceso() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LogoApp(tamano = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AdBrand.AI",
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Contenido con tu identidad",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
        ChipSuave(
            texto = "PYME",
            colorTexto = IndigoOscuro,
            colorFondo = IndigoClaro
        )
    }
}
