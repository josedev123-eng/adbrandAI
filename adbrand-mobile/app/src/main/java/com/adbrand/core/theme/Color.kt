package com.adbrand.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta de la app = la misma de la web (adbrand-web/src/index.css), con la regla 60-30-10:
// 60 % fondos claros (FondoClaro, SuperficieBlanca), 30 % índigo de marca (botones, pestaña activa, títulos
// destacados) y 10 % acentos: magenta de la IA (Crear y lo generado por IA) y coral del logo.
// Los nombres se conservan para no tocar cada pantalla; lo que cambia es el color.
val IndigoAdbrand = Color(0xFF4F46E5)
val IndigoOscuro = Color(0xFF3730A3)
val IndigoClaro = Color(0xFFECEBFD)
// Magenta de la IA (10 %): botón Crear y todo lo que genera la IA.
val VioletaAdbrand = Color(0xFFB5179E)
val VioletaClaro = Color(0xFFFBE8F8)
// Coral del logo (10 %). Para texto se usa CoralTexto, que cumple el contraste AA.
val CoralAdbrand = Color(0xFFF97316)
val CoralTexto = Color(0xFFC2410C)
val FondoClaro = Color(0xFFF4F4F9)
val SuperficieBlanca = Color(0xFFFFFFFF)
val TextoPrincipal = Color(0xFF1B1D2E)
val TextoSecundario = Color(0xFF5D6178)
val BordeSuave = Color(0xFFDFE1EC)
val Blanco = Color(0xFFFFFFFF)

// Gris para íconos sin seleccionar y detalles; superficie secundaria (chips, campos deshabilitados).
val TintaSuave = Color(0xFF7A7F98)
val PapelHueso = Color(0xFFEEEFF7)

// Estados: avisos de éxito, error y advertencia (los mismos de la web).
val ErrorRojo = Color(0xFFC0392B)
val ErrorSuave = Color(0xFFFBE6E3)
val ExitoVerde = Color(0xFF15803D)
val ExitoSuave = Color(0xFFE2F4E7)
val AmbarOscuro = Color(0xFFA15C07)
val AmbarSuave = Color(0xFFFDF0D9)

// Tarjeta principal ("hero"): índigo de marca, apenas más oscuro abajo.
val DegradadoMarca = Brush.linearGradient(
    listOf(Color(0xFF5B52EC), Color(0xFF3F37C9))
)
