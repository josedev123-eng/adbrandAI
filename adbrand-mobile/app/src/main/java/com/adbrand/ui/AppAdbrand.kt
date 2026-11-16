package com.adbrand.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adbrand.core.session.GestorSesion
import com.adbrand.core.notificaciones.GestorPush
import com.adbrand.core.notificaciones.MemoriaAvisos
import com.adbrand.ui.notificaciones.DialogoPermisoAvisos
import com.adbrand.ui.anuncio.screens.CrearAnuncioScreen
import com.adbrand.ui.auth.screens.BienvenidaScreen
import com.adbrand.ui.auth.screens.LoginScreen
import com.adbrand.ui.auth.screens.RecuperarContrasenaScreen
import com.adbrand.ui.auth.screens.RegistroScreen
import com.adbrand.ui.components.AperturaMarca
import com.adbrand.ui.components.BarraInferior
import com.adbrand.ui.components.ItemNavegacion
import com.adbrand.ui.panel.screens.PanelScreen
import com.adbrand.ui.perfil.screens.DatosEmpresaScreen
import com.adbrand.ui.perfil.screens.PerfilNegocioScreen
import com.adbrand.ui.previsualizacion.screens.PrevisualizacionScreen
import kotlinx.coroutines.launch

private const val RUTA_APERTURA = "apertura"
private const val RUTA_LOGIN = "login"
private const val RUTA_REGISTRO = "registro"
private const val RUTA_RECUPERAR = "recuperar"
private const val RUTA_BIENVENIDA = "bienvenida"
private const val RUTA_PANEL = "panel"
private const val RUTA_EMPRESA = "empresa"
private const val RUTA_CREAR = "crear"
private const val RUTA_PERFIL = "perfil"
private const val RUTA_PREVISUALIZACION = "previsualizacion"

private val DESTINOS_PRINCIPALES = setOf(
    RUTA_PANEL,
    RUTA_EMPRESA,
    RUTA_CREAR,
    RUTA_PERFIL,
    RUTA_PREVISUALIZACION
)

// HU 42: la app se presenta con una apertura de marca y, al terminar, entra al
// panel si ya hay sesión o al login si no.
@Composable
fun AppAdbrand() {
    val navegador = rememberNavController()
    val inicio = remember { RUTA_APERTURA }
    val entrada by navegador.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    // HU 50: permiso de avisos con explicación previa y sin insistir cada vez.
    var pedirPermiso by remember { mutableStateOf(false) }
    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        MemoriaAvisos.marcarPreguntado(contexto)
        if (concedido) alcance.launch { GestorPush.sincronizar(contexto) }
    }

    LaunchedEffect(rutaActual) {
        if (rutaActual in DESTINOS_PRINCIPALES) {
            GestorPush.sincronizar(contexto)
            if (!GestorPush.tienePermiso(contexto) && !MemoriaAvisos.yaPreguntado(contexto)) {
                pedirPermiso = true
            }
        }
    }

    val destinos = remember {
        listOf(
            ItemNavegacion(RUTA_PANEL, "Panel", Icons.Filled.Home),
            ItemNavegacion(RUTA_EMPRESA, "Empresa", Icons.Filled.Business),
            ItemNavegacion(RUTA_CREAR, "Crear", Icons.Filled.Add, destacado = true),
            ItemNavegacion(RUTA_PERFIL, "Perfil", Icons.Filled.Person),
            ItemNavegacion(RUTA_PREVISUALIZACION, "Vista", Icons.Filled.Visibility)
        )
    }

    fun irA(ruta: String) {
        navegador.navigate(ruta) {
            popUpTo(RUTA_PANEL) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun cerrarSesion() {
        navegador.navigate(RUTA_LOGIN) {
            popUpTo(RUTA_PANEL) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (rutaActual in DESTINOS_PRINCIPALES) {
                BarraInferior(rutaActual, destinos, ::irA)
            }
        }
    ) { relleno ->
        NavHost(
            navController = navegador,
            startDestination = inicio,
            modifier = Modifier.padding(relleno),
            enterTransition = {
                fadeIn(tween(260)) + slideInHorizontally(tween(300)) { ancho -> ancho / 10 }
            },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = {
                fadeIn(tween(240)) + slideInHorizontally(tween(280)) { ancho -> -ancho / 12 }
            },
            popExitTransition = {
                fadeOut(tween(150)) + slideOutHorizontally(tween(240)) { ancho -> ancho / 10 }
            }
        ) {
            composable(RUTA_APERTURA) {
                AperturaMarca(duracionMs = 2100L) {
                    val destino = if (GestorSesion.haySesion()) RUTA_PANEL else RUTA_LOGIN
                    navegador.navigate(destino) {
                        popUpTo(RUTA_APERTURA) { inclusive = true }
                    }
                }
            }

            composable(RUTA_LOGIN) {
                LoginScreen(
                    alAutenticarse = {
                        navegador.navigate(RUTA_BIENVENIDA) {
                            popUpTo(RUTA_LOGIN) { inclusive = true }
                        }
                    },
                    irARegistro = { navegador.navigate(RUTA_REGISTRO) },
                    irARecuperar = { navegador.navigate(RUTA_RECUPERAR) }
                )
            }

            composable(RUTA_REGISTRO) {
                RegistroScreen(alVolver = { navegador.popBackStack() })
            }

            composable(RUTA_RECUPERAR) {
                RecuperarContrasenaScreen(alVolver = { navegador.popBackStack() })
            }

            composable(RUTA_BIENVENIDA) {
                BienvenidaScreen {
                    navegador.navigate(RUTA_PANEL) {
                        popUpTo(RUTA_BIENVENIDA) { inclusive = true }
                    }
                }
            }

            composable(RUTA_PANEL) {
                PanelScreen(alCrearAnuncio = { irA(RUTA_CREAR) })
            }

            composable(RUTA_EMPRESA) {
                DatosEmpresaScreen(alCerrarSesion = ::cerrarSesion)
            }

            composable(RUTA_CREAR) {
                CrearAnuncioScreen(alIrAPerfil = { irA(RUTA_PERFIL) })
            }

            composable(RUTA_PERFIL) {
                PerfilNegocioScreen()
            }

            composable(RUTA_PREVISUALIZACION) {
                PrevisualizacionScreen(alCrearAnuncio = { irA(RUTA_CREAR) })
            }
        }

        DialogoPermisoAvisos(
            visible = pedirPermiso,
            alActivar = {
                pedirPermiso = false
                lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            alCerrar = {
                pedirPermiso = false
                MemoriaAvisos.marcarPreguntado(contexto)
            }
        )
    }
}
