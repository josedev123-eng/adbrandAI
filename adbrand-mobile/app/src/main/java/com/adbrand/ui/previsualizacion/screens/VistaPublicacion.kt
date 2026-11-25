package com.adbrand.ui.previsualizacion.screens

import android.graphics.Bitmap
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
import com.adbrand.domain.model.KitMarca
import com.adbrand.ui.previsualizacion.recorte.AjusteRecorte
import com.adbrand.ui.previsualizacion.recorte.ExportadorAnuncio
import com.adbrand.ui.previsualizacion.recorte.MarcaImagen
import com.adbrand.ui.previsualizacion.recorte.MensajeCompartir
import com.adbrand.ui.previsualizacion.recorte.Recorte

// HU 46: formatos disponibles para el selector (código, nombre y para qué sirve).
internal val FORMATOS = listOf(
    Triple("CUADRADO", "Cuadrado", "Publicación"),
    Triple("VERTICAL", "Vertical", "Historia"),
    Triple("HORIZONTAL", "Horizontal", "Portada")
)

// HU 46, criterio 1: selector de formato (cuadrado, vertical u horizontal).
@Composable
fun SelectorFormato(actual: String, habilitado: Boolean, alElegir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("¿Dónde lo vas a usar?", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FORMATOS.forEach { (codigo, nombre, uso) ->
                FilterChip(
                    selected = actual == codigo,
                    enabled = habilitado,
                    onClick = { alElegir(codigo) },
                    label = {
                        Column {
                            Text(uso, style = MaterialTheme.typography.labelLarge)
                            Text(nombre.lowercase(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }
        }
    }
}

// HU 46, criterio 2: aviso cuando el texto se acortó con "…" para entrar en el formato.
@Composable
// Es una nota discreta (gris), no una alerta: el anuncio no tiene ningún problema, solo se acortó para el tamaño.
fun AvisoRecorte(formato: FormatoVista) {
    Text(
        text = "Texto acortado para el formato ${formato.nombre.lowercase()} (hasta ${formato.maxCaracteres} caracteres).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// HU 46: el anuncio dibujado en el formato elegido (cuadrado, vertical u horizontal) con la imagen, el logo,
// los colores y la tipografía del kit. No imita ninguna red social: solo muestra cómo queda el anuncio en ese tamaño.
@Composable
fun VistaPublicacion(
    anuncio: Anuncio,
    kit: KitMarca?,
    formato: FormatoVista,
    imagen: String?,
    cargandoImagen: Boolean,
    errorImagen: String?,
    alCambiarFormato: (String) -> Unit,
    alCerrar: () -> Unit,
    // HU 48: zoom y recorte.
    ajuste: AjusteRecorte = AjusteRecorte(),
    mensajeCompartir: String? = null,
    alAjustar: (zoom: Float, arrastreX: Float, arrastreY: Float, anchoMarco: Float, anchoImagen: Int, altoImagen: Int) -> Unit = { _, _, _, _, _, _ -> },
    alRestablecer: () -> Unit = {},
    // HU 48: guardar en la galería. HU 47: generar la imagen y abrir el menú de compartir.
    alGuardar: (guardar: () -> Boolean) -> Unit = {},
    alCompartir: (generar: () -> Uri?, abrirMenu: (Uri) -> Unit) -> Unit = { _, _ -> },
    // Listo para publicar: texto con hashtags y "Pide por WhatsApp". Si no llegó, se comparte el texto del anuncio.
    textoPublicar: String? = null,
    alAvisar: (String) -> Unit = {},
    // HU 64: "Tu oferta, sin multas".
    observacionesLegales: List<Pair<String, String>> = emptyList(),
    // HU 65: video para Reels y Estados.
    creandoVideo: Boolean = false,
    alCrearVideo: (alListo: (ByteArray) -> Unit) -> Unit = {}
) {
    val bitmap = remember(imagen) { imagen?.let(::decodificarBitmap) }
    val contexto = LocalContext.current
    val acento = coloresDe(kit?.paletaColores.orEmpty()).getOrNull(1) ?: Color(0xFFF97316)
    val principal = colorPrincipal(kit)
    val nombreArchivo = "adbrand_${anuncio.id ?: 0}_${formato.codigo.lowercase()}"
    val logoKit = remember(kit?.logoImagen) { kit?.logoImagen?.let(::decodificarBitmap) }

    // La imagen del anuncio con la marca, recortada como se ve en el marco (HU 46, 47 y 48).
    fun imagenFinal(): Bitmap {
        val recorte = bitmap?.let { Recorte.rectangulo(ajuste, it.width, it.height, proporcion(formato)) }
        val logo = kit?.logoDiseno
        val nombre = nombreDe(kit)
        val marca = MarcaImagen(
            nombreNegocio = nombre,
            textoLogo = logo?.texto?.ifBlank { null } ?: nombre.take(2).uppercase(),
            colorFondoLogo = colorDesde(logo?.colorFondo, principal).toArgb(),
            colorTextoLogo = colorDesde(logo?.colorTexto, Color.White).toArgb(),
            colorPrincipal = principal.toArgb(),
            colorAcento = acento.toArgb(),
            // HU 61: el logo del kit (subido o con IA) y el eslogan elegido.
            logo = logoKit,
            eslogan = kit?.eslogan.orEmpty()
        )
        return ExportadorAnuncio.crearImagen(bitmap, recorte, formato.ancho, formato.alto, anuncio.textoAjustado, marca)
    }
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF2F3F7)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tu anuncio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${formato.nombre} · ${formato.ancho} × ${formato.alto} px",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = alCerrar) { Text("Cerrar") }
                }

                SelectorFormato(actual = formato.codigo, habilitado = true, alElegir = alCambiarFormato)

                if (kit == null) {
                    Aviso("Todavía no tienes kit de marca: se usan colores por defecto. Genera tu kit para ver tu logo y tus colores.")
                }

                AnuncioEnFormato(anuncio, kit, formato, bitmap, cargandoImagen, ajuste, alAjustar)

                // HU 48: acercar, mover, restablecer y guardar la imagen recortada.
                if (bitmap != null && !cargandoImagen) {
                    Text(
                        "Pellizca con dos dedos para acercar o alejar y arrastra para mover. El recorte queda dentro del marco.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = alRestablecer, enabled = !ajuste.esOriginal, modifier = Modifier.weight(1f)) {
                            Text("Restablecer")
                        }
                        OutlinedButton(
                            onClick = { alGuardar { ExportadorAnuncio.guardarEnGaleria(contexto, imagenFinal(), nombreArchivo) } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Guardar") }
                    }
                }

                // HU 47: compartir la imagen del anuncio con la marca por WhatsApp, correo u otra aplicación.
                Button(
                    onClick = {
                        alCompartir(
                            { ExportadorAnuncio.prepararParaCompartir(contexto, imagenFinal(), nombreArchivo) },
                            { uri ->
                                // Instagram no recibe el texto al compartir: se copia solo para pegarlo en la descripción.
                                textoPublicar?.let { copiarAlPortapapeles(contexto, it) }
                                ExportadorAnuncio.abrirMenuCompartir(
                                    contexto, uri,
                                    textoPublicar?.let { MensajeCompartir("Anuncio de ${nombreDe(kit)}", it) }
                                        ?: MensajeCompartir.para(nombreDe(kit), anuncio.textoAjustado, kit?.eslogan.orEmpty())
                                )
                            }
                        )
                    },
                    enabled = !cargandoImagen,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Compartir (WhatsApp, Instagram…)") }
                // HU 64: lo que falta según las reglas de publicidad de INDECOPI (fecha de vigencia, stock…).
                if (observacionesLegales.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Tu oferta, sin multas (reglas de INDECOPI)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            observacionesLegales.forEach { (titulo, detalle) ->
                                Text("• $titulo. $detalle", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Agrégalo al texto antes de publicar. Son recomendaciones, no una asesoría legal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (textoPublicar != null) {
                    OutlinedButton(
                        onClick = {
                            copiarAlPortapapeles(contexto, textoPublicar)
                            alAvisar("Texto con hashtags copiado. Pégalo en la descripción de tu publicación.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copiar texto con hashtags") }
                    Text(
                        "Al compartir, el texto con tus hashtags y tu WhatsApp se copia solo: pégalo en Instagram o Facebook.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // HU 65: video vertical de 8 s con la imagen, el texto y la marca, listo para Reels o Estados.
                OutlinedButton(
                    onClick = {
                        alCrearVideo { bytes ->
                            textoPublicar?.let { copiarAlPortapapeles(contexto, it) }
                            compartirVideo(contexto, bytes, "anuncio_${anuncio.id ?: 0}", textoPublicar ?: anuncio.textoAjustado)
                        }
                    },
                    enabled = !creandoVideo,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (creandoVideo) "Creando tu video…" else "🎬 Video para Reels y Estados") }
                if (creandoVideo) {
                    Text(
                        "Armando tu video de 8 segundos con tu marca… suele tardar menos de medio minuto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                mensajeCompartir?.let { Aviso(it) }

                if (anuncio.recortado) AvisoRecorte(formato)
                when {
                    errorImagen != null -> Aviso("No se pudo cargar la imagen: $errorImagen")
                    !cargandoImagen && imagen == null ->
                        Aviso("Este anuncio no tiene imagen; se muestra un diseño con los colores de tu marca.")
                }
            }
        }
    }
}

// El anuncio en el formato elegido: la imagen (o un fondo con los colores del kit) con la proporción del formato,
// el logo del kit arriba y el texto con la tipografía de la marca. Solo cambia la forma y dónde va el texto.
@Composable
private fun AnuncioEnFormato(
    anuncio: Anuncio,
    kit: KitMarca?,
    formato: FormatoVista,
    bitmap: Bitmap?,
    cargando: Boolean,
    ajuste: AjusteRecorte,
    alAjustar: (Float, Float, Float, Float, Int, Int) -> Unit
) {
    val principal = colorPrincipal(kit)
    val acento = coloresDe(kit?.paletaColores.orEmpty()).getOrNull(1) ?: Color(0xFFF97316)
    val nombre = nombreDe(kit)
    val vertical = formato.codigo == "VERTICAL"
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (vertical) 0.72f else 1f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 3.dp
        ) {
            Box {
                ImagenAnuncio(anuncio, kit, bitmap, cargando, proporcion(formato), ajuste, alAjustar)
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LogoMarca(kit?.logoDiseno, nombre, principal, tamano = if (vertical) 34.dp else 38.dp, imagen = kit?.logoImagen)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            nombre,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = familiaDe(kit?.tipografiaPrimaria.orEmpty()),
                            fontWeight = FontWeight.Bold
                        )
                        // HU 61: el eslogan del kit.
                        kit?.eslogan?.ifBlank { null }?.let { eslogan ->
                            Text(
                                "“$eslogan”",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = familiaDe(kit.tipografiaSecundaria),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
                        .padding(16.dp),
                    horizontalAlignment = if (vertical) Alignment.CenterHorizontally else Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = anuncio.textoAjustado,
                        color = Color.White,
                        style = if (formato.codigo == "HORIZONTAL") MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontFamily = familiaDe(kit?.tipografiaPrimaria.orEmpty()),
                        fontWeight = FontWeight.Bold,
                        textAlign = if (vertical) TextAlign.Center else TextAlign.Start
                    )
                    Box(modifier = Modifier.size(width = 44.dp, height = 5.dp).background(acento, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

// La imagen del anuncio con la proporción del formato. Sin imagen (o con la de prueba en SVG, que Android no dibuja),
// un diseño con los colores y la tipografía del kit.
@Composable
private fun ImagenAnuncio(
    anuncio: Anuncio,
    kit: KitMarca?,
    bitmap: Bitmap?,
    cargando: Boolean,
    proporcion: Float,
    ajuste: AjusteRecorte,
    alAjustar: (Float, Float, Float, Float, Int, Int) -> Unit,
    mostrarTexto: Boolean = false
) {
    val imagenDibujable = remember(bitmap) { bitmap?.asImageBitmap() }
    var anchoMarco by remember { mutableFloatStateOf(0f) }
    val colores = coloresDe(kit?.paletaColores.orEmpty())
    val fondo = colores.firstOrNull() ?: Color(0xFF4F46E5)
    val acento = colores.getOrNull(1) ?: Color(0xFFF97316)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(proporcion)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        when {
            cargando -> CircularProgressIndicator(color = Color.White)
            bitmap != null && imagenDibujable != null -> {
                // HU 48: se dibuja solo la parte recortada (según el zoom y la posición) llenando el marco.
                val recorte = Recorte.rectangulo(ajuste, bitmap.width, bitmap.height, proporcion)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { anchoMarco = it.width.toFloat() }
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, arrastre, zoom, _ ->
                                alAjustar(zoom, arrastre.x, arrastre.y, anchoMarco, bitmap.width, bitmap.height)
                            }
                        }
                ) {
                    drawImage(
                        image = imagenDibujable,
                        srcOffset = IntOffset(recorte.izquierda, recorte.arriba),
                        srcSize = IntSize(recorte.ancho, recorte.alto),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                }
            }
            mostrarTexto -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    text = anuncio.textoAjustado.take(70),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = familiaDe(kit?.tipografiaPrimaria.orEmpty()),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(12.dp))
                Box(modifier = Modifier.size(width = 48.dp, height = 6.dp).background(acento, RoundedCornerShape(3.dp)))
            }
            else -> Box(modifier = Modifier.size(width = 64.dp, height = 8.dp).background(acento, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(texto, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun colorPrincipal(kit: KitMarca?): Color =
    coloresDe(kit?.paletaColores.orEmpty()).firstOrNull() ?: MaterialTheme.colorScheme.primary

private fun nombreDe(kit: KitMarca?): String = kit?.nombreNegocio?.ifBlank { null } ?: "Tu negocio"

// Ancho / alto del formato: 1 (cuadrado), 0.5625 (vertical 9:16), 1.91 (horizontal).
internal fun proporcion(formato: FormatoVista): Float =
    if (formato.alto <= 0) 1f else formato.ancho.toFloat() / formato.alto.toFloat()

// "data:image/jpeg;base64,..." → imagen. null si no es una imagen que Android pueda dibujar (por ejemplo, SVG).
internal fun decodificarBitmap(dataUrl: String): Bitmap? = runCatching {
    val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()

// Copia el texto listo para publicar (Instagram no lo recibe al compartir una imagen).
private fun copiarAlPortapapeles(contexto: android.content.Context, texto: String) {
    val portapapeles = contexto.getSystemService(android.content.ClipboardManager::class.java) ?: return
    portapapeles.setPrimaryClip(android.content.ClipData.newPlainText("Anuncio", texto))
}

// HU 65: guarda el video en la caché de la app y abre el menú para compartirlo (WhatsApp, Instagram, TikTok…).
private fun compartirVideo(contexto: android.content.Context, bytes: ByteArray, nombre: String, texto: String) {
    val carpeta = java.io.File(contexto.cacheDir, "compartidas").apply { mkdirs() }
    val archivo = java.io.File(carpeta, "$nombre.mp4").apply { writeBytes(bytes) }
    val uri = androidx.core.content.FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
    val intento = android.content.Intent(android.content.Intent.ACTION_SEND)
        .setType("video/mp4")
        .putExtra(android.content.Intent.EXTRA_STREAM, uri)
        .putExtra(android.content.Intent.EXTRA_TEXT, texto)
        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    contexto.startActivity(
        android.content.Intent.createChooser(intento, "Compartir video").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
