package com.adbrand.ui.metricas.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// HU 59: abre o comparte el PDF descargado con otras apps del celular.
// El FileProvider (AndroidManifest) entrega un enlace temporal al archivo, sin exponer la carpeta de la app.

private const val TIPO_PDF = "application/pdf"

private fun enlace(contexto: Context, archivo: File): Uri =
    FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)

// Abre el PDF con el lector del celular. Devuelve false si no hay ninguna app que abra PDF.
fun abrirPdf(contexto: Context, archivo: File): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(enlace(contexto, archivo), TIPO_PDF)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        contexto.startActivity(intent)
        true
    } catch (error: ActivityNotFoundException) {
        false
    }
}

// Abre el menú para enviar el PDF por WhatsApp, correo u otra app.
fun compartirPdf(contexto: Context, archivo: File) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType(TIPO_PDF)
        .putExtra(Intent.EXTRA_STREAM, enlace(contexto, archivo))
        .putExtra(Intent.EXTRA_SUBJECT, "Informe de campañas de AdBrand")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    contexto.startActivity(Intent.createChooser(intent, "Compartir reporte"))
}
