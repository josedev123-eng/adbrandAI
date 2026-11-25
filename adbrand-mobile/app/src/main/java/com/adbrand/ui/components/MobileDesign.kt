package com.adbrand.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbrand.core.theme.AmbarOscuro
import com.adbrand.core.theme.AmbarSuave
import com.adbrand.core.theme.Blanco
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.DegradadoMarca
import com.adbrand.core.theme.ErrorRojo
import com.adbrand.core.theme.ErrorSuave
import com.adbrand.core.theme.ExitoSuave
import com.adbrand.core.theme.ExitoVerde
import com.adbrand.core.theme.FondoClaro
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.PapelHueso
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.TintaSuave
import com.adbrand.core.theme.VioletaAdbrand

// ---------------------------------------------------------------------------
// Marca
// ---------------------------------------------------------------------------

/** Isotipo de la app: cuadrado tinta con las iniciales en serif. */
@Composable
fun LogoApp(
    modifier: Modifier = Modifier,
    tamano: Dp = 56.dp,
    sombra: Boolean = true,
    texto: String = "AB",
    brush: Brush = DegradadoMarca,
    colorTexto: Color = Blanco
) {
    val forma = RoundedCornerShape(tamano * 0.24f)
    Box(
        modifier = modifier
            .size(tamano)
            .then(
                if (sombra) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = forma,
                        ambientColor = IndigoOscuro.copy(alpha = 0.30f),
                        spotColor = IndigoOscuro.copy(alpha = 0.30f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(forma)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = (tamano.value * 0.36f).sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Estructura de pantalla
// ---------------------------------------------------------------------------

/** Fondo base de toda la pantalla: papel con un velo apenas perceptible arriba. */
@Composable
fun FondoPantalla(modifier: Modifier = Modifier, contenido: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(FondoClaro)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(IndigoClaro.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
        contenido()
    }
}

/**
 * Fotografía tratada como material impreso: se tiñe en tono papel para no
 * romper la paleta y se enmarca con la misma línea fina que las tarjetas.
 */
@Composable
fun FotoEditorial(
    @DrawableRes recurso: Int,
    modifier: Modifier = Modifier,
    radio: Dp = 10.dp,
    velo: Boolean = false,
    borde: Boolean = true,
    contenido: @Composable BoxScope.() -> Unit = {}
) {
    val forma = RoundedCornerShape(radio)
    Box(
        modifier = modifier
            .clip(forma)
            .background(PapelHueso)
            .then(if (borde) Modifier.border(1.dp, BordeSuave, forma) else Modifier)
    ) {
        Image(
            painter = painterResource(recurso),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(PapelHueso, BlendMode.Multiply)
        )
        if (velo) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.40f to Color.Transparent,
                            1f to SuperficieBlanca.copy(alpha = 0.94f)
                        )
                    )
            )
        }
        contenido()
    }
}

/**
 * Pantalla con desplazamiento vertical que respeta la barra de estado.
 * El contenido se recorta bajo la barra para que nunca se vea "detrás" de los iconos.
 */
@Composable
fun PantallaScroll(
    modifier: Modifier = Modifier,
    fondo: Color = FondoClaro,
    rellenoHorizontal: Dp = 20.dp,
    espaciado: Dp = 0.dp,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = rellenoHorizontal),
                verticalArrangement = Arrangement.spacedBy(espaciado),
                content = contenido
            )
        }
    }
}

/** Título grande con subtítulo y, opcionalmente, una acción a la derecha. */
@Composable
fun EncabezadoPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Regla corta en terracota: firma editorial de los encabezados.
        Box(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .size(width = 30.dp, height = 2.dp)
                .background(VioletaAdbrand)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineMedium,
                color = TextoPrincipal,
                modifier = Modifier.weight(1f)
            )
            accion?.invoke()
        }
        if (!subtitulo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
    }
}

/** Tarjeta blanca con encabezado (icono + título + subtítulo) y contenido. */
@Composable
fun TarjetaSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    icono: ImageVector? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(320)),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icono != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndigoClaro),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = IndigoAdbrand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextoPrincipal
                    )
                    if (!subtitulo.isNullOrBlank()) {
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }
            }
            contenido()
        }
    }
}

/** Etiqueta pequeña tipo "chip" con fondo suave. */
@Composable
fun ChipSuave(
    texto: String,
    modifier: Modifier = Modifier,
    colorTexto: Color = IndigoOscuro,
    colorFondo: Color = IndigoClaro
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colorFondo)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = colorTexto
        )
    }
}

// ---------------------------------------------------------------------------
// Formulario
// ---------------------------------------------------------------------------

/** Campo de texto consistente para todos los formularios. */
@Composable
fun CampoAdbrand(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    error: String? = null,
    ayuda: String? = null,
    marcador: String? = null,
    habilitado: Boolean = true,
    unaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    accionIme: ImeAction = ImeAction.Next,
    transformacion: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier.fillMaxWidth(),
        enabled = habilitado,
        label = { Text(etiqueta) },
        placeholder = marcador?.let { texto -> { Text(texto) } },
        leadingIcon = if (icono != null && unaLinea) {
            {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            null
        },
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(error) }
            }
            ayuda != null -> {
                { Text(ayuda) }
            }
            else -> null
        },
        singleLine = unaLinea,
        minLines = lineasMinimas,
        visualTransformation = transformacion,
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accionIme),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IndigoAdbrand,
            unfocusedBorderColor = BordeSuave,
            disabledBorderColor = BordeSuave,
            focusedContainerColor = SuperficieBlanca,
            unfocusedContainerColor = SuperficieBlanca,
            disabledContainerColor = PapelHueso,
            focusedLeadingIconColor = IndigoAdbrand,
            unfocusedLeadingIconColor = TextoSecundario,
            disabledLeadingIconColor = TextoSecundario,
            focusedLabelColor = IndigoAdbrand,
            cursorColor = IndigoAdbrand
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

/** Botón principal: tinta sólida, esquina corta y respuesta táctil discreta. */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    habilitado: Boolean = true,
    icono: ImageVector? = null
) {
    val activo = habilitado && !cargando
    val forma = RoundedCornerShape(12.dp)
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.985f else 1f,
        animationSpec = tween(120),
        label = "boton-presionado"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(if (activo) IndigoAdbrand else BordeSuave)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = activo,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Blanco
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = texto,
                    color = if (activo) Blanco else TextoSecundario,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (icono != null) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = if (activo) Blanco else TextoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Botón secundario: superficie blanca con borde, para acciones alternativas. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true
) {
    val forma = RoundedCornerShape(12.dp)
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.97f else 1f,
        animationSpec = tween(120),
        label = "boton-secundario-presionado"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(SuperficieBlanca)
            .border(1.dp, BordeSuave, forma)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (habilitado) IndigoOscuro else TextoSecundario.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (habilitado) IndigoOscuro else TextoSecundario.copy(alpha = 0.5f)
        )
    }
}

enum class TipoAviso { INFO, EXITO, ERROR }

/** Aviso en línea para mensajes de éxito o error del backend. */
@Composable
fun AvisoEstado(
    texto: String,
    modifier: Modifier = Modifier,
    tipo: TipoAviso = TipoAviso.INFO
) {
    val fondo = when (tipo) {
        TipoAviso.EXITO -> ExitoSuave
        TipoAviso.ERROR -> ErrorSuave
        TipoAviso.INFO -> IndigoClaro
    }
    val color = when (tipo) {
        TipoAviso.EXITO -> ExitoVerde
        TipoAviso.ERROR -> ErrorRojo
        TipoAviso.INFO -> IndigoOscuro
    }
    val icono = when (tipo) {
        TipoAviso.EXITO -> Icons.Filled.CheckCircle
        TipoAviso.ERROR -> Icons.Filled.Warning
        TipoAviso.INFO -> Icons.Filled.Info
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = texto,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Texto centrado para estados vacíos. */
@Composable
fun TextoCentrado(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = TextoSecundario
) {
    Text(
        text = texto,
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

/** Halo radial suave, sin bordes visibles: decora fondos y héroes. */
@Composable
fun HaloMarca(
    modifier: Modifier = Modifier,
    color: Color = IndigoAdbrand,
    alpha: Float = 0.22f,
    tamano: Dp = 320.dp
) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(
                Brush.radialGradient(
                    listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.45f),
                        Color.Transparent
                    )
                )
            )
    )
}

/** Círculo con icono, usado en estados vacíos. */
@Composable
fun IconoDestacado(
    icono: ImageVector,
    modifier: Modifier = Modifier,
    colorFondo: Color = IndigoClaro,
    colorIcono: Color = IndigoAdbrand,
    tamano: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.22f))
            .background(colorFondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = colorIcono,
            modifier = Modifier.size(tamano * 0.46f)
        )
    }
}

// ---------------------------------------------------------------------------
// Navegación
// ---------------------------------------------------------------------------

data class ItemNavegacion(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
    val destacado: Boolean = false
)

/** Barra inferior: papel opaco, línea superior fina y marcador terracota. */
@Composable
fun BarraInferior(
    rutaActual: String?,
    items: List<ItemNavegacion>,
    alNavegar: (String) -> Unit
) {
    Surface(
        color = SuperficieBlanca,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                DestinoInferior(
                    item = item,
                    seleccionado = rutaActual == item.ruta,
                    modifier = Modifier.weight(1f),
                    alTocar = { alNavegar(item.ruta) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.DestinoInferior(
    item: ItemNavegacion,
    seleccionado: Boolean,
    modifier: Modifier,
    alTocar: () -> Unit
) {
    val colorIcono by animateColorAsState(
        targetValue = if (seleccionado) IndigoAdbrand else TintaSuave,
        label = "nav-icono"
    )
    val escala by animateFloatAsState(
        targetValue = if (seleccionado) 1f else 0.96f,
        animationSpec = tween(180),
        label = "nav-escala"
    )
    val punto by animateDpAsState(
        targetValue = if (seleccionado && !item.destacado) 14.dp else 0.dp,
        animationSpec = tween(220),
        label = "nav-punto"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = alTocar)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (item.destacado) {
            // Acción principal: sello cuadrado en tinta, sin halo.
            Box(
                modifier = Modifier
                    .size(if (seleccionado) 46.dp else 44.dp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (seleccionado) IndigoAdbrand else IndigoOscuro),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icono,
                    contentDescription = item.etiqueta,
                    tint = PapelHueso,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = item.icono,
                contentDescription = item.etiqueta,
                tint = colorIcono,
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                    }
            )
        }
        Text(
            text = item.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = if (seleccionado) IndigoAdbrand else TintaSuave,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .size(width = punto, height = 2.dp)
                .background(VioletaAdbrand)
        )
    }
}

// ---------------------------------------------------------------------------
// Piezas compartidas por todas las pantallas
// ---------------------------------------------------------------------------

/**
 * Tarjeta principal con el degradado de marca, halo decorativo y logo.
 * La usan el panel, la empresa, el perfil y la vista previa para que el
 * "hero" se sienta idéntico en toda la app.
 */
@Composable
fun TarjetaHero(
    etiqueta: String,
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    iniciales: String = "AB",
    contenido: (@Composable ColumnScope.() -> Unit)? = null
) {
    // Sin halos ni brillos: una tarjeta de tinta con una regla que la firma.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(320)),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.background(DegradadoMarca)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LogoApp(
                        tamano = 46.dp,
                        texto = iniciales,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        sombra = false
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 1.dp)
                                    .background(Blanco.copy(alpha = 0.55f))
                            )
                            Text(
                                text = etiqueta,
                                style = MaterialTheme.typography.labelSmall,
                                color = Blanco.copy(alpha = 0.82f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Blanco,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!subtitulo.isNullOrBlank()) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Blanco.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                contenido?.invoke(this)
            }
        }
    }
}

/** Entrada escalonada reutilizable: fade + deslizamiento suave. */
@Composable
fun EntradaAnimada(
    indice: Int = 0,
    modifier: Modifier = Modifier,
    retrasoMs: Long = 60L,
    contenido: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(indice) {
        kotlinx.coroutines.delay((indice * retrasoMs).coerceAtMost(360L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(360)) + slideInVertically(tween(420)) { alto -> alto / 7 }
    ) {
        contenido()
    }
}

/** Etiqueta de estado de campaña o anuncio, con sus colores semánticos. */
@Composable
fun PildoraEstado(estado: String, modifier: Modifier = Modifier) {
    val fondo: Color
    val color: Color
    when (estado.uppercase()) {
        "APROBADO" -> {
            fondo = ExitoSuave
            color = ExitoVerde
        }

        "DUDOSO" -> {
            fondo = AmbarSuave
            color = AmbarOscuro
        }

        "RECHAZADO" -> {
            fondo = ErrorSuave
            color = ErrorRojo
        }

        else -> {
            fondo = IndigoClaro
            color = IndigoOscuro
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = when (estado.uppercase()) {
                "APROBADO" -> "Aprobado"
                "DUDOSO" -> "En revisión"
                "RECHAZADO" -> "Rechazado"
                else -> estado.lowercase().replaceFirstChar { it.uppercase() }
            },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/** Filtro tipo píldora con transición de color al seleccionar. */
@Composable
fun PildoraFiltro(
    texto: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    alElegir: () -> Unit
) {
    val fondo by animateColorAsState(
        targetValue = if (seleccionado) IndigoAdbrand else SuperficieBlanca,
        label = "filtro-fondo-compartido"
    )
    val colorTexto by animateColorAsState(
        targetValue = if (seleccionado) Blanco else TextoSecundario,
        label = "filtro-texto-compartido"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = fondo,
        border = if (seleccionado) null else BorderStroke(1.dp, BordeSuave)
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = alElegir)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = colorTexto
            )
        }
    }
}

/** Botón circular con icono, para refrescar o volver. */
@Composable
fun BotonIcono(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorIcono: Color = IndigoAdbrand
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = colorIcono,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Bloque gris animado que imita la forma del contenido mientras carga. */
@Composable
fun Esqueleto(
    modifier: Modifier = Modifier,
    altura: Dp = 14.dp,
    forma: Shape = RoundedCornerShape(8.dp)
) {
    val infinito = rememberInfiniteTransition(label = "esqueleto")
    val avance by infinito.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "esqueleto-avance"
    )

    Box(
        modifier = modifier
            .height(altura)
            .clip(forma)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BordeSuave.copy(alpha = 0.55f),
                        PapelHueso,
                        BordeSuave.copy(alpha = 0.55f)
                    ),
                    start = Offset(avance * 900f - 450f, 0f),
                    end = Offset(avance * 900f, 0f)
                )
            )
    )
}

/** Estado vacío compartido: icono, título, descripción y una acción opcional. */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconoDestacado(icono = icono)
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                textAlign = TextAlign.Center
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
            accion?.invoke()
        }
    }
}
