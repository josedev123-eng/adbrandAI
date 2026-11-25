package com.adbrand.ui.previsualizacion.recorte

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File

// Datos de la marca que se dibujan en la imagen del anuncio.
data class MarcaImagen(
    val nombreNegocio: String,
    val textoLogo: String,
    val colorFondoLogo: Int,
    val colorTextoLogo: Int,
    val colorPrincipal: Int,
    val colorAcento: Int,
    // HU 61: el logo del kit como imagen (subido o con IA) y el eslogan. Sin logo se dibujan las iniciales.
    val logo: Bitmap? = null,
    val eslogan: String = ""
)

// HU 47 y 48: arma la imagen del anuncio con la marca (tamaño del formato) para guardarla (HU 48) o compartirla (HU 47).
object ExportadorAnuncio {

    private const val LADO_MAXIMO = 1080

    // La imagen del anuncio: la foto recortada (o, si no tiene, el color principal de la marca), el logo y el nombre
    // del negocio arriba, el texto abajo y una barrita del color de acento.
    fun crearImagen(
        original: Bitmap?,
        recorte: RectanguloRecorte?,
        anchoFormato: Int,
        altoFormato: Int,
        texto: String,
        marca: MarcaImagen
    ): Bitmap {
        val factor = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(anchoFormato, altoFormato))
        val ancho = (anchoFormato * factor).toInt().coerceAtLeast(1)
        val alto = (altoFormato * factor).toInt().coerceAtLeast(1)

        val salida = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(salida)
        if (original != null && recorte != null) {
            val origen = Rect(recorte.izquierda, recorte.arriba, recorte.izquierda + recorte.ancho, recorte.arriba + recorte.alto)
            lienzo.drawBitmap(original, origen, Rect(0, 0, ancho, alto), Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            lienzo.drawColor(marca.colorPrincipal)
        }

        val corto = minOf(ancho, alto).toFloat()
        val margen = ancho * 0.06f

        // Sombra arriba y abajo para que el logo y el texto se lean sobre cualquier foto.
        lienzo.drawRect(0f, 0f, ancho.toFloat(), alto * 0.22f, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, alto * 0.22f, 0x80000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
        })
        lienzo.drawRect(0f, alto * 0.55f, ancho.toFloat(), alto.toFloat(), Paint().apply {
            shader = LinearGradient(0f, alto * 0.55f, 0f, alto.toFloat(), 0x00000000, 0xB3000000.toInt(), Shader.TileMode.CLAMP)
        })

        // Logo del kit (imagen en un círculo blanco, o cuadro redondeado con las iniciales) y nombre del negocio.
        val ladoLogo = corto * 0.11f
        val logo = RectF(margen, margen, margen + ladoLogo, margen + ladoLogo)
        if (marca.logo != null) {
            val borde = ladoLogo * 0.06f
            lienzo.drawOval(RectF(logo.left - borde, logo.top - borde, logo.right + borde, logo.bottom + borde),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
            lienzo.save()
            lienzo.clipPath(android.graphics.Path().apply { addOval(logo, android.graphics.Path.Direction.CW) })
            lienzo.drawBitmap(marca.logo, recorteCuadrado(marca.logo), logo, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            lienzo.restore()
        } else {
            lienzo.drawRoundRect(logo, ladoLogo * 0.25f, ladoLogo * 0.25f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = marca.colorFondoLogo })
            val pinturaLogo = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = marca.colorTextoLogo
                typeface = Typeface.DEFAULT_BOLD
                textSize = ladoLogo * 0.42f
                textAlign = Paint.Align.CENTER
            }
            lienzo.drawText(marca.textoLogo.take(3), logo.centerX(), logo.centerY() - (pinturaLogo.descent() + pinturaLogo.ascent()) / 2, pinturaLogo)
        }
        val pinturaNombre = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = corto * 0.042f
        }
        val anchoNombre = ancho - logo.right - margen * 2
        val nombre = TextUtils.ellipsize(marca.nombreNegocio, pinturaNombre, anchoNombre, TextUtils.TruncateAt.END)
        val eslogan = marca.eslogan.trim().trim('"', '“', '”')
        if (eslogan.isEmpty()) {
            lienzo.drawText(nombre.toString(), logo.right + margen * 0.5f, logo.centerY() - (pinturaNombre.descent() + pinturaNombre.ascent()) / 2, pinturaNombre)
        } else {
            // HU 61: el nombre arriba y el eslogan en cursiva debajo, a la altura del logo.
            lienzo.drawText(nombre.toString(), logo.right + margen * 0.5f, logo.centerY() - pinturaNombre.descent(), pinturaNombre)
            val pinturaEslogan = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = corto * 0.03f
            }
            val lema = TextUtils.ellipsize("“$eslogan”", pinturaEslogan, anchoNombre, TextUtils.TruncateAt.END)
            lienzo.drawText(lema.toString(), logo.right + margen * 0.5f, logo.centerY() - pinturaEslogan.ascent() + ladoLogo * 0.04f, pinturaEslogan)
        }

        // Texto del anuncio abajo.
        val pintura = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = corto * 0.065f
        }
        val diseno = StaticLayout.Builder
            .obtain(texto, 0, texto.length, pintura, (ancho - 2 * margen).toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(4)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        val barra = pintura.textSize * 0.35f
        lienzo.save()
        lienzo.translate(margen, alto - margen - diseno.height - barra * 2)
        diseno.draw(lienzo)
        lienzo.restore()
        lienzo.drawRect(margen, alto - margen - barra, margen + pintura.textSize * 2.2f, alto - margen, Paint().apply { color = marca.colorAcento })
        return salida
    }

    // El cuadrado central de la imagen, para que el logo llene el círculo sin deformarse.
    private fun recorteCuadrado(imagen: Bitmap): Rect {
        val lado = minOf(imagen.width, imagen.height)
        val x = (imagen.width - lado) / 2
        val y = (imagen.height - lado) / 2
        return Rect(x, y, x + lado, y + lado)
    }

    // HU 48: guarda la imagen en la galería (Imágenes/AdBrand). Desde Android 10 no pide permisos.
    fun guardarEnGaleria(contexto: Context, imagen: Bitmap, nombreArchivo: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val valores = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$nombreArchivo.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AdBrand")
            }
            val uri = contexto.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores) ?: return false
            contexto.contentResolver.openOutputStream(uri)?.use { imagen.compress(Bitmap.CompressFormat.JPEG, 92, it) } ?: return false
            true
        }.getOrDefault(false)
    }

    // HU 47: guarda la imagen en la caché de la app y devuelve su dirección para compartirla (sin pedir permisos).
    fun prepararParaCompartir(contexto: Context, imagen: Bitmap, nombreArchivo: String): Uri {
        val carpeta = File(contexto.cacheDir, "compartidas").apply { mkdirs() }
        val archivo = File(carpeta, "$nombreArchivo.jpg")
        archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
    }

    // HU 47: abre el menú de compartir del celular (WhatsApp, correo, Instagram, etc.) con la imagen,
    // el asunto (para el correo) y el texto del mensaje.
    fun abrirMenuCompartir(contexto: Context, uri: Uri, mensaje: MensajeCompartir) {
        val compartir = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, mensaje.asunto)
            putExtra(Intent.EXTRA_TEXT, mensaje.cuerpo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(
            Intent.createChooser(compartir, "Compartir anuncio").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
