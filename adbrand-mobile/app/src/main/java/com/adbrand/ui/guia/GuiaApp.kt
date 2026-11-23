package com.adbrand.ui.guia

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adbrand.core.theme.BordeSuave
import com.adbrand.core.theme.IndigoAdbrand
import com.adbrand.core.theme.IndigoClaro
import com.adbrand.core.theme.SuperficieBlanca
import com.adbrand.core.theme.TextoPrincipal
import com.adbrand.core.theme.TextoSecundario
import com.adbrand.core.theme.VioletaAdbrand
import com.adbrand.ui.components.BotonPrincipal

private data class PasoGuia(val icono: ImageVector, val pestana: String?, val titulo: String, val texto: String)

// La app explicada en 5 pasos cortos, en el orden en que se usa: crear → revisar → publicar → medir → tu marca.
private val PASOS = listOf(
    PasoGuia(
        Icons.Filled.Waves, null,
        "¡Hola! Soy Bran",
        "Te ayudo a crear la publicidad de tu negocio con inteligencia artificial. Te muestro la app en 1 minuto."
    ),
    PasoGuia(
        Icons.Filled.Add, "Crear",
        "1. Crea un anuncio",
        "Toca + Crear y escribe qué quieres vender, por ejemplo \"2 panetones por S/ 35\". Yo te propongo 4 anuncios con texto e imagen."
    ),
    PasoGuia(
        Icons.Filled.Collections, "Anuncios",
        "2. Elige y publica",
        "En \"Por revisar\" te quedas con los que te gusten. En \"Listos para publicar\" eliges si va en publicación, historia o portada, y lo compartes."
    ),
    PasoGuia(
        Icons.Filled.BarChart, "Métricas",
        "3. Mira cómo te va",
        "Cuántos anuncios creaste y cuántos están listos. Puedes descargar tu informe en PDF."
    ),
    PasoGuia(
        Icons.Filled.Person, "Cuenta",
        "4. Tu marca, a tu estilo",
        "Completa tu perfil y crea tu kit de marca: así escribo con tu tono y uso tu logo y tus colores. ¡Eso es todo!"
    )
)

/** Guía de bienvenida con el robot. "alTerminar" se llama al cerrarla (Saltar o ¡Empezar!). */
@Composable
fun GuiaApp(alTerminar: () -> Unit) {
    var paso by rememberSaveable { mutableIntStateOf(0) }
    val ultimo = paso == PASOS.lastIndex

    Dialog(
        onDismissRequest = alTerminar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = SuperficieBlanca, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (!ultimo) TextButton(onClick = alTerminar) { Text("Saltar", color = TextoSecundario) }
                    }

                    RobotGuia(tamano = 150.dp, saludando = paso == 0 || ultimo)

                    AnimatedContent(
                        targetState = paso,
                        transitionSpec = {
                            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                        },
                        label = "paso-guia"
                    ) { indice ->
                        val actual = PASOS[indice]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (actual.pestana != null) {
                                // Dónde está esa parte en el menú de abajo.
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (actual.pestana == "Crear") VioletaAdbrand else IndigoClaro)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val color = if (actual.pestana == "Crear") Color.White else IndigoAdbrand
                                    Icon(actual.icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    Text("Pestaña ${actual.pestana}", style = MaterialTheme.typography.labelLarge, color = color)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                actual.titulo,
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextoPrincipal,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                actual.texto,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextoSecundario,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Puntos(total = PASOS.size, actual = paso)
                    Spacer(modifier = Modifier.height(18.dp))

                    BotonPrincipal(
                        texto = if (ultimo) "¡Empezar!" else "Siguiente",
                        onClick = { if (ultimo) alTerminar() else paso++ }
                    )
                    if (paso > 0) {
                        TextButton(onClick = { paso-- }) { Text("Atrás", color = TextoSecundario) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Puntos(total: Int, actual: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { indice ->
            val ancho by animateDpAsState(if (indice == actual) 22.dp else 8.dp, label = "punto")
            Box(
                modifier = Modifier
                    .size(width = ancho, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (indice == actual) IndigoAdbrand else BordeSuave)
            )
        }
    }
}

/** Recuerda en el teléfono si ya se vio la guía (se muestra sola solo la primera vez). */
object MemoriaGuia {
    private const val ARCHIVO = "adbrand_guia"
    private const val CLAVE = "vista"

    fun yaVista(contexto: Context): Boolean =
        contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean(CLAVE, false)

    fun marcarVista(contexto: Context) {
        contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, true).apply()
    }
}
