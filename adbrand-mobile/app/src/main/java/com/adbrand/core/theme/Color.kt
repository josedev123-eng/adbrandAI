package com.adbrand.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta de la app: papel cálido, tinta verde y terracota.
// El kit de marca del negocio puede reemplazarla en la previsualización.
val IndigoAdbrand = Color(0xFF1F3D33)
val IndigoOscuro = Color(0xFF10251D)
val IndigoClaro = Color(0xFFE5EBE1)
val VioletaAdbrand = Color(0xFFB4512E)
val VioletaClaro = Color(0xFFF7E7DD)
val CoralAdbrand = Color(0xFFC77A33)
val FondoClaro = Color(0xFFF4F0E6)
val SuperficieBlanca = Color(0xFFFFFCF4)
val TextoPrincipal = Color(0xFF191710)
val TextoSecundario = Color(0xFF6B6456)
val BordeSuave = Color(0xFFE0D8C7)
val Blanco = Color(0xFFFFFFFF)

// Segunda tinta para detalles editoriales (reglas, numeración, sellos).
val TintaSuave = Color(0xFF8A8375)
val PapelHueso = Color(0xFFFAF6EC)

// Estados: avisos de éxito, error y advertencia.
val ErrorRojo = Color(0xFF9C3A2A)
val ErrorSuave = Color(0xFFF9E7E1)
val ExitoVerde = Color(0xFF2F6B4F)
val ExitoSuave = Color(0xFFE7F0E7)
val AmbarOscuro = Color(0xFF95661D)
val AmbarSuave = Color(0xFFF9F0DC)

// Firma visual: verde tinta profundo, casi plano. Sin brillos ni violetas.
val DegradadoMarca = Brush.linearGradient(
    listOf(Color(0xFF24493C), Color(0xFF17332A))
)
