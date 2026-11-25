package com.adbrand.ui.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.adbrand.ui.auth.viewmodel.AccesoCuentaViewModel
import com.adbrand.ui.components.AperturaMarca
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.CampoAdbrand
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.HaloMarca
import com.adbrand.ui.components.LogoApp
import com.adbrand.ui.components.TipoAviso
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Splash animado al iniciar sesión
// ---------------------------------------------------------------------------

@Composable
fun BienvenidaScreen(alContinuar: () -> Unit) {
    // Misma composici�n que la apertura, m�s breve: el login ya se celebr�.
    AperturaMarca(duracionMs = 1200L, alTerminar = alContinuar)
}

// ---------------------------------------------------------------------------
// Registro
// ---------------------------------------------------------------------------

@Composable
fun RegistroScreen(alVolver: () -> Unit, viewModel: AccesoCuentaViewModel = viewModel()) {
    val s by viewModel.estado.collectAsState()

    MarcoAuth(
        etiqueta = "EMPIEZA TU MARCA",
        titulo = if (s.terminado) "Tu cuenta está lista" else "Crea tu cuenta",
        subtitulo = if (s.terminado) {
            "Ya puedes entrar y empezar a crear contenido con tu identidad."
        } else {
            "Un espacio para organizar tu negocio y crear con identidad."
        },
        alVolver = alVolver
    ) {
        if (!s.terminado) {
            CampoAdbrand(
                valor = s.nombre,
                alCambiar = viewModel::nombre,
                etiqueta = "Tu nombre",
                icono = Icons.Filled.Person
            )
            CampoAdbrand(
                valor = s.empresa,
                alCambiar = viewModel::empresa,
                etiqueta = "Nombre de tu empresa",
                icono = Icons.Filled.Storefront
            )
            CampoAdbrand(
                valor = s.correo,
                alCambiar = viewModel::correo,
                etiqueta = "Correo",
                icono = Icons.Filled.Email,
                marcador = "hola@tunegocio.pe",
                tipoTeclado = KeyboardType.Email,
                accionIme = ImeAction.Next
            )
            CampoAdbrand(
                valor = s.contrasena,
                alCambiar = viewModel::contrasena,
                etiqueta = "Contraseña",
                icono = Icons.Filled.Lock,
                ayuda = "8 caracteres o más, con mayúscula, minúscula y número.",
                tipoTeclado = KeyboardType.Password,
                accionIme = ImeAction.Next,
                transformacion = PasswordVisualTransformation()
            )
            CampoAdbrand(
                valor = s.repetir,
                alCambiar = viewModel::repetir,
                etiqueta = "Repite la contraseña",
                icono = Icons.Filled.Lock,
                tipoTeclado = KeyboardType.Password,
                accionIme = ImeAction.Done,
                transformacion = PasswordVisualTransformation()
            )
        }

        s.error?.let { AvisoEstado(texto = it, tipo = TipoAviso.ERROR) }
        s.mensaje?.let { AvisoEstado(texto = it, tipo = TipoAviso.EXITO) }

        if (s.terminado) {
            BotonPrincipal(
                texto = "Ir a iniciar sesión",
                onClick = alVolver,
                icono = Icons.Filled.ArrowForward
            )
        } else {
            BotonPrincipal(
                texto = "Crear cuenta",
                onClick = viewModel::registrar,
                cargando = s.cargando,
                icono = Icons.Filled.ArrowForward
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Recuperar contraseña
// ---------------------------------------------------------------------------

@Composable
fun RecuperarContrasenaScreen(alVolver: () -> Unit, viewModel: AccesoCuentaViewModel = viewModel()) {
    val s by viewModel.estado.collectAsState()
    val titulo = if (s.terminado) "Contraseña actualizada" else "Recuperar contraseña"
    val ayuda = when {
        s.terminado -> "Ya puedes entrar con tu contraseña nueva."
        s.paso == 1 -> "Te enviaremos un código de seis dígitos para confirmar tu cuenta."
        else -> "Escribe el código y elige una contraseña nueva."
    }

    MarcoAuth(
        etiqueta = "RECUPERA EL ACCESO",
        titulo = titulo,
        subtitulo = ayuda,
        alVolver = alVolver
    ) {
        if (!s.terminado) {
            CampoAdbrand(
                valor = s.correo,
                alCambiar = viewModel::correo,
                etiqueta = "Correo de tu cuenta",
                icono = Icons.Filled.Email,
                tipoTeclado = KeyboardType.Email,
                accionIme = if (s.paso == 1) ImeAction.Done else ImeAction.Next
            )
            if (s.paso == 2) {
                CampoAdbrand(
                    valor = s.codigo,
                    alCambiar = viewModel::codigo,
                    etiqueta = "Código de seis dígitos",
                    icono = Icons.Filled.Lock,
                    marcador = "000000",
                    tipoTeclado = KeyboardType.Number,
                    ayuda = "Revisa tu correo: el código vence en unos minutos.",
                    accionIme = ImeAction.Next
                )
                CampoAdbrand(
                    valor = s.nueva,
                    alCambiar = viewModel::nueva,
                    etiqueta = "Contraseña nueva",
                    icono = Icons.Filled.Lock,
                    tipoTeclado = KeyboardType.Password,
                    accionIme = ImeAction.Next,
                    transformacion = PasswordVisualTransformation()
                )
                CampoAdbrand(
                    valor = s.repetirNueva,
                    alCambiar = viewModel::repetirNueva,
                    etiqueta = "Repite la contraseña",
                    icono = Icons.Filled.Lock,
                    tipoTeclado = KeyboardType.Password,
                    accionIme = ImeAction.Done,
                    transformacion = PasswordVisualTransformation()
                )
            }
        }

        s.error?.let { AvisoEstado(texto = it, tipo = TipoAviso.ERROR) }
        s.mensaje?.let { AvisoEstado(texto = it, tipo = TipoAviso.EXITO) }

        when {
            s.terminado -> BotonPrincipal(
                texto = "Volver a iniciar sesión",
                onClick = alVolver,
                icono = Icons.Filled.ArrowForward
            )

            s.paso == 1 -> BotonPrincipal(
                texto = "Enviar código",
                onClick = viewModel::pedirCodigo,
                cargando = s.cargando,
                icono = Icons.Filled.ArrowForward
            )

            else -> {
                BotonPrincipal(
                    texto = "Guardar contraseña nueva",
                    onClick = viewModel::cambiarContrasena,
                    cargando = s.cargando,
                    icono = Icons.Filled.ArrowForward
                )
                TextButton(
                    onClick = viewModel::otroCodigo,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Pedir otro código",
                        style = MaterialTheme.typography.labelLarge,
                        color = IndigoOscuro
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Marco común de las pantallas de cuenta
// ---------------------------------------------------------------------------

@Composable
private fun MarcoAuth(
    etiqueta: String,
    titulo: String,
    subtitulo: String,
    alVolver: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    FondoAuth {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            EntradaAnimada(indice = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuperficieBlanca,
                        border = BorderStroke(1.dp, BordeSuave)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable(onClick = alVolver),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = IndigoOscuro,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "AdBrand.AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = IndigoAdbrand,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            EntradaAnimada(indice = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        color = IndigoAdbrand
                    )
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextoPrincipal
                    )
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            EntradaAnimada(indice = 3) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SuperficieBlanca,
                    border = BorderStroke(1.dp, BordeSuave)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                        content = contenido
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            EntradaAnimada(indice = 4) {
                TextButton(
                    onClick = alVolver,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Volver a iniciar sesión",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Fondo y animación
// ---------------------------------------------------------------------------

@Composable
private fun FondoAuth(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PapelHueso, FondoClaro, Color(0xFFEFEADD))
                )
            )
    ) {
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
