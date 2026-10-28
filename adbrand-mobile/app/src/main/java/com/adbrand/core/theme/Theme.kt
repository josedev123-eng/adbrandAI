package com.adbrand.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaAdbrand = lightColorScheme(
    primary = IndigoAdbrand,
    onPrimary = Blanco,
    primaryContainer = IndigoClaro,
    onPrimaryContainer = IndigoOscuro,
    secondary = CoralAdbrand,
    onSecondary = Blanco,
    background = FondoClaro,
    onBackground = TextoPrincipal,
    surface = Blanco,
    onSurface = TextoPrincipal,
    surfaceVariant = FondoClaro,
    onSurfaceVariant = TextoSecundario,
    outline = BordeSuave,
    error = ErrorRojo
)

@Composable
fun AdbrandTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaAdbrand,
        typography = AdbrandTypography,
        content = contenido
    )
}
