package com.adbrand

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.adbrand.core.notificaciones.AvisosPush
import com.adbrand.core.notificaciones.BuzonNotificacion
import com.adbrand.core.theme.AdbrandTheme
import com.adbrand.ui.AppAdbrand

// HU 42: punto de entrada de la app. La navegación vive en AppAdbrand.
// HU 50: si la app se abre tocando una notificación, el aviso se entrega al buzón.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app es siempre clara: barras transparentes con iconos oscuros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        BuzonNotificacion.abrirSiHaceFalta(AvisosPush.desde(intent))
        setContent {
            AdbrandTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppAdbrand()
                }
            }
        }
    }

    // HU 50: con la app en segundo plano la notificación reutiliza esta actividad;
    // el nuevo intent trae el aviso y se navega sin recrear la pantalla.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        BuzonNotificacion.abrirSiHaceFalta(AvisosPush.desde(intent))
    }
}
