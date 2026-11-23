package com.adbrand.ui.previsualizacion.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.adbrand.core.theme.IndigoOscuro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.TintaSuave
import com.adbrand.core.theme.VioletaAdbrand
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
import com.adbrand.domain.model.KitMarca
import com.adbrand.domain.model.LogoMarca
import com.adbrand.ui.components.AvisoEstado
import com.adbrand.ui.components.BotonIcono
import com.adbrand.ui.components.BotonPrincipal
import com.adbrand.ui.components.BotonSecundario
import com.adbrand.ui.components.ChipSuave
import com.adbrand.ui.components.EncabezadoPantalla
import com.adbrand.ui.components.EntradaAnimada
import com.adbrand.ui.components.Esqueleto
import com.adbrand.ui.components.EstadoVacio
import com.adbrand.ui.components.FOTO_PORTADA
import com.adbrand.ui.components.FotoEditorial
import com.adbrand.ui.components.LogoApp
import com.adbrand.ui.components.PildoraEstado
import com.adbrand.ui.components.TarjetaHero
import com.adbrand.ui.components.TipoAviso
import com.adbrand.ui.previsualizacion.viewmodel.PrevisualizacionViewModel

// HU 45: aplica el logo, los colores y la tipografía del kit. Si no hay kit, muestra un aviso.
// HU 46, 47 y 48: selector de formato, y al tocar un anuncio se abre en ese formato (zoom, recorte, guardar y compartir).
@Composable
fun PrevisualizacionScreen(
    alCrearAnuncio: () -> Unit = {},
    // Dentro de la pestaña "Anuncios" el título y la tarjeta grande ya los pone esa pantalla.
    conEncabezado: Boolean = true,
    // Anuncio que se abre directo (por ejemplo, al tocarlo en el panel de inicio).
    abrirId: Long? = null,
    alCrearKit: () -> Unit = {},
    viewModel: PrevisualizacionViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    var abiertoDesdeFuera by remember { mutableStateOf(false) }
    LaunchedEffect(abrirId, estado.anuncios) {
        if (abrirId == null || abiertoDesdeFuera) return@LaunchedEffect
        estado.anuncios.find { it.id == abrirId }?.let {
            abiertoDesdeFuera = true
            viewModel.abrirPublicacion(it)
        }
    }

    val kit = estado.kit

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClaro)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (conEncabezado) item {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                EntradaAnimada(0) {
                    EncabezadoPantalla(
                        titulo = "Vista de marca",
                        subtitulo = "Así se ve tu identidad en cada publicación.",
                        accion = {
                            BotonIcono(
                                icono = Icons.Filled.Refresh,
                                descripcion = "Actualizar vista",
                                onClick = viewModel::cargar
                            )
                        }
                    )
                }
            }
        }

        if (conEncabezado) item {
            EntradaAnimada(1) {
                TarjetaHero(
                    etiqueta = "VISTA DE MARCA",
                    titulo = kit?.nombreNegocio?.takeIf { it.isNotBlank() }
                        ?: "Tu contenido, a tu estilo",
                    subtitulo = "Revisa cómo se ve tu identidad en cada publicación.",
                    iniciales = inicialesDe(kit?.nombreNegocio.orEmpty())
                ) {
                    val colores = coloresDe(kit?.paletaColores.orEmpty())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipSuave(
                            texto = kit?.sector?.ifBlank { "IDENTIDAD VISUAL" }
                                ?: "IDENTIDAD VISUAL",
                            colorTexto = Blanco,
                            colorFondo = Color.White.copy(alpha = 0.18f)
                        )
                        if (colores.isNotEmpty()) {
                            ChipSuave(
                                texto = "${colores.size} colores",
                                colorTexto = Blanco,
                                colorFondo = Color.White.copy(alpha = 0.18f)
                            )
                        }
                    }
                }
            }
        }

        kit?.let { marca ->
            item {
                EntradaAnimada(2) { PortadaMarca(marca) }
            }
        }

        // HU 46, criterio 1: selector de formato (cuadrado, vertical u horizontal).
        item {
            EntradaAnimada(2) {
                SelectorFormato(actual = estado.formato.codigo, habilitado = !estado.cargando, alElegir = viewModel::cambiarFormato)
            }
        }

        if (!estado.cargado && estado.error == null) {
            item { EntradaAnimada(2) { VistaCargando() } }
        }

        estado.error?.let { mensaje ->
            item {
                EntradaAnimada(2) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AvisoEstado(texto = mensaje, tipo = TipoAviso.ERROR)
                        BotonSecundario(
                            texto = "Reintentar",
                            onClick = viewModel::cargar,
                            icono = Icons.Filled.Refresh
                        )
                    }
                }
            }
        }

        if (!estado.cargando && estado.cargado) {
            if (kit == null) {
                item {
                    EntradaAnimada(2) {
                        EstadoVacio(
                            icono = Icons.Filled.Palette,
                            titulo = "Aún no tienes kit de marca",
                            descripcion = "Créalo en 1 minuto y tus anuncios usarán tu logo, tus colores y tus letras."
                        ) {
                            BotonPrincipal(texto = "Crear mi kit", onClick = alCrearKit, icono = Icons.Filled.Palette)
                        }
                    }
                }
            } else {
                item { EntradaAnimada(2) { EncabezadoKit(kit) } }
                item { EntradaAnimada(3) { MuestraTipografia(kit) } }
            }

            if (estado.anuncios.isEmpty()) {
                item {
                    EntradaAnimada(4) {
                        EstadoVacio(
                            icono = Icons.Filled.Star,
                            titulo = "Todavía no generaste anuncios",
                            descripcion = "Crea tu primer anuncio y aquí verás cómo se ve con la identidad de tu marca."
                        ) {
                            BotonPrincipal(
                                texto = "Crear anuncio",
                                onClick = alCrearAnuncio,
                                icono = Icons.Filled.Add
                            )
                        }
                    }
                }
            } else {
                item {
                    EntradaAnimada(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tus publicaciones",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextoPrincipal
                            )
                            ChipSuave(texto = "${estado.anuncios.size}")
                        }
                    }
                }
                item {
                    Text(
                        text = "Toca un anuncio para verlo en el formato elegido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
                itemsIndexed(estado.anuncios, key = { _, anuncio -> anuncio.id ?: anuncio.hashCode().toLong() }) { _, anuncio ->
                    EntradaAnimada(5) {
                        TarjetaAnuncio(anuncio, kit, estado.formato, alTocar = { viewModel.abrirPublicacion(anuncio) })
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(26.dp)) }
    }

    // HU 46, 47 y 48: el anuncio abierto en el formato elegido.
    estado.publicacion?.let { anuncio ->
        VistaPublicacion(
            anuncio = anuncio,
            kit = estado.kit,
            formato = estado.formato,
            imagen = estado.imagen,
            cargandoImagen = estado.cargandoImagen,
            errorImagen = estado.errorImagen,
            alCambiarFormato = viewModel::cambiarFormato,
            alCerrar = viewModel::cerrarPublicacion,
            ajuste = estado.ajuste,
            mensajeCompartir = estado.mensajeCompartir,
            alAjustar = viewModel::ajustar,
            alRestablecer = viewModel::restablecer,
            alGuardar = { guardar -> viewModel.guardar(guardar) },
            alCompartir = { generar, abrirMenu -> viewModel.compartir(generar, abrirMenu) },
            textoPublicar = estado.textoPublicar,
            alAvisar = viewModel::avisar
        )
    }
}

// ---------------------------------------------------------------------------
// Estados de carga
// ---------------------------------------------------------------------------

@Composable
private fun VistaCargando() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    modifier = Modifier.size(60.dp),
                    altura = 60.dp,
                    forma = RoundedCornerShape(16.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Esqueleto(modifier = Modifier.fillMaxWidth(0.6f), altura = 13.dp)
                    Esqueleto(modifier = Modifier.fillMaxWidth(0.4f), altura = 11.dp)
                }
            }
            Esqueleto(modifier = Modifier.fillMaxWidth(), altura = 12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    Esqueleto(
                        modifier = Modifier.size(32.dp),
                        altura = 32.dp,
                        forma = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kit de marca
// ---------------------------------------------------------------------------

@Composable
private fun PortadaMarca(kit: KitMarca) {
    val iniciales = inicialesDe(kit.nombreNegocio)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.height(212.dp)) {
            FotoEditorial(
                recurso = FOTO_PORTADA,
                modifier = Modifier.matchParentSize(),
                radio = 0.dp,
                borde = false
            )
            // La foto se disuelve en la tarjeta: nada de bordes duros.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.34f to Color.Transparent,
                            1f to SuperficieBlanca
                        )
                    )
            )
            // Velo superior para que la etiqueta se lea sobre la foto.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(IndigoOscuro.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
            Text(
                text = "PORTADA · 01",
                style = MaterialTheme.typography.labelSmall,
                color = Blanco,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LogoApp(tamano = 38.dp, texto = iniciales)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IDENTIDAD DE MARCA",
                        style = MaterialTheme.typography.labelSmall,
                        color = TintaSuave
                    )
                    Text(
                        text = kit.nombreNegocio.ifBlank { "Tu negocio" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EncabezadoKit(kit: KitMarca) {
    val colores = coloresDe(kit.paletaColores)
    val principal = colores.firstOrNull() ?: IndigoAdbrand
    val secundario = colores.getOrNull(1) ?: VioletaAdbrand

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogoMarca(kit.logoDiseno, kit.nombreNegocio, principal)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kit.nombreNegocio.ifBlank { "Tu negocio" },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrincipal,
                        fontFamily = familiaDe(kit.tipografiaPrimaria),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = kit.sector.ifBlank { "Tu rubro" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }

            if (colores.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colores.take(5).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color)
                                .border(1.dp, BordeSuave, RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                EtiquetaColor(texto = "Color principal", color = principal)
                EtiquetaColor(texto = "Acento", color = secundario)
            }

            if (kit.vozMarca.isNotBlank()) {
                Text(
                    text = "Voz de marca: ${kit.vozMarca}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
        }
    }
}

@Composable
private fun EtiquetaColor(texto: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = TextoSecundario
        )
    }
}

@Composable
private fun MuestraTipografia(kit: KitMarca) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ASÍ SE VE TU TIPOGRAFÍA",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Text(
                text = kit.nombreNegocio.ifBlank { "Tu marca" },
                style = MaterialTheme.typography.headlineMedium,
                color = TextoPrincipal,
                fontFamily = familiaDe(kit.tipografiaPrimaria),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = kit.logoConcepto.ifBlank { "Un anuncio con tu identidad visual." },
                style = MaterialTheme.typography.bodyLarge,
                color = TextoSecundario,
                fontFamily = familiaDe(kit.tipografiaSecundaria)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Publicación
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaAnuncio(anuncio: Anuncio, kit: KitMarca?, formato: FormatoVista, alTocar: () -> Unit) {
    val colores = coloresDe(kit?.paletaColores.orEmpty())
    val acento = colores.firstOrNull() ?: IndigoAdbrand

    Surface(
        onClick = alTocar,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SuperficieBlanca,
        border = BorderStroke(1.dp, BordeSuave),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(acento)
                    )
                    Text(
                        text = anuncio.redSocial,
                        style = MaterialTheme.typography.labelLarge,
                        color = acento
                    )
                }
                PildoraEstado(anuncio.estado)
            }
            Text(
                text = anuncio.textoAjustado,
                style = MaterialTheme.typography.bodyLarge,
                color = TextoPrincipal,
                fontFamily = familiaDe(kit?.tipografiaSecundaria.orEmpty())
            )
            // HU 46, criterio 2: aviso si el texto se acortó para entrar en el formato.
            if (anuncio.recortado) AvisoRecorte(formato)
            Text(
                text = "Tono ${anuncio.tono.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Logo del kit
// ---------------------------------------------------------------------------

@Composable
internal fun LogoMarca(logo: LogoMarca?, nombre: String, colorPrincipal: Color, tamano: Dp = 60.dp) {
    val texto = logo?.texto?.ifBlank { null } ?: nombre.take(2).uppercase().ifBlank { "AB" }
    val fondo = colorDesde(logo?.colorFondo, colorPrincipal)
    val colorTexto = colorDesde(logo?.colorTexto, Color.White)
    val forma = when (logo?.forma) {
        "CIRCULO" -> CircleShape
        "CUADRADO" -> RectangleShape
        else -> RoundedCornerShape8
    }

    Box(
        modifier = Modifier
            .size(tamano)
            .clip(forma)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = colorTexto,
            style = if (tamano < 48.dp) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleLarge,
            fontFamily = familiaDe(logo?.tipografia.orEmpty()),
            fontWeight = FontWeight.Bold
        )
    }
}

private val RoundedCornerShape8 = RoundedCornerShape(16.dp)

internal fun colorDesde(hex: String?, porDefecto: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(porDefecto)

internal fun coloresDe(paleta: String): List<Color> =
    Regex("#[0-9A-Fa-f]{6}")
        .findAll(paleta)
        .mapNotNull { runCatching { Color(android.graphics.Color.parseColor(it.value)) }.getOrNull() }
        .distinct()
        .take(5)
        .toList()

internal fun familiaDe(nombre: String): FontFamily = when {
    nombre.contains("mono", ignoreCase = true) -> FontFamily.Monospace
    nombre.contains("serif", ignoreCase = true) ||
        nombre.contains("playfair", ignoreCase = true) ||
        nombre.contains("georgia", ignoreCase = true) -> FontFamily.Serif

    else -> FontFamily.SansSerif
}

private fun inicialesDe(nombre: String): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.isEmpty() -> "AB"
        palabras.size == 1 -> palabras.first().take(2).uppercase()
        else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
    }
}
