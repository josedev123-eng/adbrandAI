package com.adbrand.ui.previsualizacion.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
import com.adbrand.domain.model.KitMarca

// HU 46: formatos disponibles para el selector (código, nombre y para qué sirve).
internal val FORMATOS = listOf(
    Triple("CUADRADO", "Cuadrado", "Publicación"),
    Triple("VERTICAL", "Vertical", "Historia"),
    Triple("HORIZONTAL", "Horizontal", "Horizontal")
)

// HU 46, criterio 1: selector de formato (cuadrado, vertical u horizontal).
@Composable
fun SelectorFormato(actual: String, habilitado: Boolean, alElegir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Formato", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FORMATOS.forEach { (codigo, nombre, uso) ->
                FilterChip(
                    selected = actual == codigo,
                    enabled = habilitado,
                    onClick = { alElegir(codigo) },
                    label = {
                        Column {
                            Text(nombre, style = MaterialTheme.typography.labelLarge)
                            if (uso != nombre) Text(uso, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }
        }
    }
}

// HU 46, criterio 2: aviso cuando el texto se acortó con "…" para entrar en el formato.
@Composable
fun AvisoRecorte(formato: FormatoVista) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFDF0D9)) {
        Text(
            text = "El texto no entra completo en el formato ${formato.nombre.lowercase()} " +
                "(máximo ${formato.maxCaracteres} caracteres): se acortó con \"…\".",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A4B00),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
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
    alCerrar: () -> Unit
) {
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
                        Text("Vista previa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

                AnuncioEnFormato(anuncio, kit, formato, imagen, cargandoImagen)

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
private fun AnuncioEnFormato(anuncio: Anuncio, kit: KitMarca?, formato: FormatoVista, imagen: String?, cargando: Boolean) {
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
                ImagenAnuncio(anuncio, kit, imagen, cargando, proporcion(formato), mostrarTexto = false)
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LogoMarca(kit?.logoDiseno, nombre, principal, tamano = if (vertical) 34.dp else 38.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        nombre,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = familiaDe(kit?.tipografiaPrimaria.orEmpty()),
                        fontWeight = FontWeight.Bold
                    )
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
    imagen: String?,
    cargando: Boolean,
    proporcion: Float,
    mostrarTexto: Boolean = true
) {
    val bitmap = remember(imagen) { imagen?.let(::decodificar) }
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
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = "Imagen del anuncio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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
private fun decodificar(dataUrl: String): ImageBitmap? = runCatching {
    val bytes = Base64.decode(dataUrl.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()
