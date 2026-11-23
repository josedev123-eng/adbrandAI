package com.adbrand.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Esquema claro igual a la web: fondos claros, índigo de marca y magenta de la IA.
private val EsquemaAdbrand = lightColorScheme(
    primary = IndigoAdbrand,
    onPrimary = Blanco,
    primaryContainer = IndigoClaro,
    onPrimaryContainer = IndigoOscuro,
    secondary = VioletaAdbrand,
    onSecondary = Blanco,
    secondaryContainer = VioletaClaro,
    onSecondaryContainer = Color(0xFF5B0B4F),
    tertiary = CoralAdbrand,
    onTertiary = Blanco,
    background = FondoClaro,
    onBackground = TextoPrincipal,
    surface = SuperficieBlanca,
    onSurface = TextoPrincipal,
    surfaceVariant = PapelHueso,
    onSurfaceVariant = TextoSecundario,
    outline = BordeSuave,
    outlineVariant = BordeSuave,
    error = ErrorRojo,
    onError = Blanco,
    errorContainer = ErrorSuave,
    onErrorContainer = ErrorRojo
)

// Esquinas como la web: tarjetas de 12 a 16 dp.
private val FormasAdbrand = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun AdbrandTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaAdbrand,
        typography = AdbrandTypography,
        shapes = FormasAdbrand,
        content = contenido
    )
}
