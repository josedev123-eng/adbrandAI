package com.adbrand.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

// Titulares en sans y en negrita, como la web: el mismo estilo sobrio y formal en las dos.
private val Titular = FontFamily.SansSerif

val AdbrandTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Titular,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Titular,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Titular,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Titular,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.9.sp
    )
)
