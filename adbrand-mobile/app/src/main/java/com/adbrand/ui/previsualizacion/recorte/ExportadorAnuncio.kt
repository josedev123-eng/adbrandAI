package com.adbrand.ui.previsualizacion.recorte

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File

// HU 48: arma la imagen recortada del anuncio (tamaño del formato, con su texto) y la comparte.
// Se guarda en la caché de la app para compartirla (sin pedir permisos) y, desde Android 10, también en la galería
// (Imágenes/AdBrand).
object ExportadorAnuncio {

    private const val LADO_MAXIMO = 1080

    fun crearImagen(
        original: Bitmap,
        recorte: RectanguloRecorte,
        anchoFormato: Int,
        altoFormato: Int,
        texto: String,
        colorAcento: Int
    ): Bitmap {
        // Mismo tamaño que el formato, con el lado mayor en 1080 px como máximo.
        val factor = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(anchoFormato, altoFormato))
        val ancho = (anchoFormato * factor).toInt().coerceAtLeast(1)
        val alto = (altoFormato * factor).toInt().coerceAtLeast(1)

        val salida = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(salida)
        val origen = Rect(recorte.izquierda, recorte.arriba, recorte.izquierda + recorte.ancho, recorte.arriba + recorte.alto)
        lienzo.drawBitmap(original, origen, Rect(0, 0, ancho, alto), Paint(Paint.FILTER_BITMAP_FLAG))

        // Franja oscura abajo para que el texto se lea sobre cualquier imagen.
        val sombra = Paint().apply {
            shader = LinearGradient(0f, alto * 0.55f, 0f, alto.toFloat(), 0x00000000, 0xB3000000.toInt(), Shader.TileMode.CLAMP)
        }
        lienzo.drawRect(0f, alto * 0.55f, ancho.toFloat(), alto.toFloat(), sombra)

        val margen = ancho * 0.06f
        val pintura = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = minOf(ancho, alto) * 0.065f
        }
        val diseno = StaticLayout.Builder
            .obtain(texto, 0, texto.length, pintura, (ancho - 2 * margen).toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(4)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        val barra = pintura.textSize * 0.35f
        val arribaTexto = alto - margen - diseno.height - barra * 2
        lienzo.save()
        lienzo.translate(margen, arribaTexto)
        diseno.draw(lienzo)
        lienzo.restore()
        lienzo.drawRect(margen, alto - margen - barra, margen + pintura.textSize * 2.2f, alto - margen, Paint().apply { color = colorAcento })
        return salida
    }

    // Guarda y abre el menú de compartir del celular. Devuelve el mensaje para mostrar.
    fun guardarYCompartir(contexto: Context, imagen: Bitmap, nombreArchivo: String): String {
        val carpeta = File(contexto.cacheDir, "compartidas").apply { mkdirs() }
        val archivo = File(carpeta, "$nombreArchivo.jpg")
        archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.JPEG, 92, it) }

        val enGaleria = guardarEnGaleria(contexto, imagen, nombreArchivo)

        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
        val compartir = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(compartir, "Compartir anuncio").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return if (enGaleria) "Imagen guardada en tu galería (AdBrand)." else "Imagen lista para compartir."
    }

    private fun guardarEnGaleria(contexto: Context, imagen: Bitmap, nombreArchivo: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val valores = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$nombreArchivo.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AdBrand")
            }
            val uri = contexto.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores) ?: return false
            contexto.contentResolver.openOutputStream(uri)?.use { imagen.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            true
        }.getOrDefault(false)
    }
}
