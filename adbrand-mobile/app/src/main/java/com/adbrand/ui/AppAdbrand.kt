package com.adbrand.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adbrand.core.session.GestorSesion
import com.adbrand.core.notificaciones.BuzonNotificacion
import com.adbrand.core.notificaciones.GestorPush
import com.adbrand.core.notificaciones.MemoriaAvisos
import com.adbrand.core.notificaciones.RutasAviso
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AuthRepositoryRemoto
import com.adbrand.ui.notificaciones.BannerAviso
import com.adbrand.ui.notificaciones.DialogoPermisoAvisos
import com.adbrand.ui.anuncio.screens.CrearAnuncioScreen
import com.adbrand.ui.auth.screens.BienvenidaScreen
import com.adbrand.ui.auth.screens.LoginScreen
import com.adbrand.ui.auth.screens.RecuperarContrasenaScreen
import com.adbrand.ui.auth.screens.RegistroScreen
import com.adbrand.ui.avisos.screens.PreferenciasAvisosScreen
import com.adbrand.ui.campana.screens.DetalleCampanaScreen
import com.adbrand.ui.components.AperturaMarca
import com.adbrand.ui.components.BarraInferior
import com.adbrand.ui.components.ItemNavegacion
import com.adbrand.ui.panel.screens.PanelScreen
import com.adbrand.ui.perfil.screens.DatosEmpresaScreen
import com.adbrand.ui.perfil.screens.PerfilNegocioScreen
import com.adbrand.ui.plan.screens.PantallaPlanScreen
import com.adbrand.ui.previsualizacion.screens.PrevisualizacionScreen
import kotlinx.coroutines.delay
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
private const val RUTA_CAMPANA = "campana/{id}"
private const val RUTA_PLAN = "plan"
private const val RUTA_AVISOS = "avisos"

private val DESTINOS_PRINCIPALES = setOf(
    RUTA_PANEL,
    RUTA_EMPRESA,
    RUTA_CREAR,
    RUTA_PERFIL,
    RUTA_PREVISUALIZACION
)

// HU 50: mientras el usuario está en apertura o cuenta no se salta a un aviso;
// el destino queda pendiente y se abre al entrar al panel.
private val DESTINOS_SIN_AVISOS = setOf(
    RUTA_APERTURA,
    RUTA_LOGIN,
    RUTA_REGISTRO,
    RUTA_RECUPERAR,
    RUTA_BIENVENIDA
)

// HU 55: usuario demo con el que la app inicia sesión automáticamente al correr.
private const val DEMO_CORREO = "demo@adbrand.ai"
private const val DEMO_CONTRASENA = "Demo1234"

// HU 42: la app se presenta con una apertura de marca y, al terminar, entra al
// panel si ya hay sesión o al login si no; HU 55: si no hay sesión, arranca con
// el usuario demo automáticamente.
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

    // HU 50: aviso en primer plano y navegación al tocar una notificación.
    val aviso by BuzonNotificacion.aviso.collectAsState()
    val destinoPendiente by BuzonNotificacion.destino.collectAsState()

    LaunchedEffect(aviso?.id) {
        if (aviso != null) {
            delay(6500)
            BuzonNotificacion.cerrarBanner()
        }
    }

    LaunchedEffect(destinoPendiente?.id, rutaActual) {
        val pendiente = destinoPendiente ?: return@LaunchedEffect
        if (rutaActual == null || rutaActual in DESTINOS_SIN_AVISOS) return@LaunchedEffect

        BuzonNotificacion.consumirDestino()
        val ruta = RutasAviso.destino(pendiente)
        if (ruta != null && ruta != rutaActual) {
            runCatching {
                navegador.navigate(ruta) { launchSingleTop = true }
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    if (GestorSesion.haySesion()) {
                        // HU 42: si ya hay sesión guardada, entra al panel.
                        navegador.navigate(RUTA_PANEL) {
                            popUpTo(RUTA_APERTURA) { inclusive = true }
                        }
                    } else {
                        // HU 55: cuando la app corre y no hay sesión, inicia sesión
                        // automáticamente con el usuario demo del entorno local.
                        alcance.launch {
                            val resultado = AuthRepositoryRemoto().iniciarSesion(DEMO_CORREO, DEMO_CONTRASENA)
                            val destino = if (resultado is Resultado.Exito) RUTA_PANEL else RUTA_LOGIN
                            navegador.navigate(destino) {
                                popUpTo(RUTA_APERTURA) { inclusive = true }
                            }
                        }
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
                PerfilNegocioScreen(alAbrirAvisos = { navegador.navigate(RUTA_AVISOS) })
            }

            composable(RUTA_PREVISUALIZACION) {
                PrevisualizacionScreen(alCrearAnuncio = { irA(RUTA_CREAR) })
            }

            // HU 49: ficha de campaña que se abre desde el aviso de recordatorio.
            composable(
                route = RUTA_CAMPANA,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                val id = entrada.arguments?.getLong("id") ?: 0L
                DetalleCampanaScreen(
                    id = id,
                    alVolver = { navegador.popBackStack() }
                )
            }

            // HU 52: pantalla del plan que se abre desde el aviso de vencimiento.
            composable(RUTA_PLAN) {
                PantallaPlanScreen(alVolver = { navegador.popBackStack() })
            }

            // HU 51: preferencias de avisos desde el perfil de la empresa.
            composable(RUTA_AVISOS) {
                PreferenciasAvisosScreen(alVolver = { navegador.popBackStack() })
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

        BannerAviso(
            aviso = aviso,
            modifier = Modifier.align(Alignment.TopCenter),
            alAbrir = { BuzonNotificacion.abrir(it) },
            alCerrar = { BuzonNotificacion.cerrarBanner() }
        )
    }
}
