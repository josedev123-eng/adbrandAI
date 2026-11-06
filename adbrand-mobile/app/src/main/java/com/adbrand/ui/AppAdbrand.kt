package com.adbrand.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adbrand.core.session.GestorSesion
import com.adbrand.ui.auth.screens.BienvenidaScreen
import com.adbrand.ui.auth.screens.LoginScreen
import com.adbrand.ui.auth.screens.RecuperarContrasenaScreen
import com.adbrand.ui.auth.screens.RegistroScreen
import com.adbrand.ui.components.BarraInferior
import com.adbrand.ui.components.ItemNavegacion
import com.adbrand.ui.perfil.screens.DatosEmpresaScreen
import com.adbrand.ui.perfil.screens.PerfilNegocioScreen
import com.adbrand.ui.previsualizacion.screens.PrevisualizacionScreen

private const val RUTA_LOGIN = "login"
private const val RUTA_REGISTRO = "registro"
private const val RUTA_RECUPERAR = "recuperar"
private const val RUTA_BIENVENIDA = "bienvenida"
private const val RUTA_EMPRESA = "empresa"
private const val RUTA_PERFIL = "perfil"
private const val RUTA_PREVISUALIZACION = "previsualizacion"
private val DESTINOS_PRINCIPALES = setOf(RUTA_EMPRESA, RUTA_PERFIL, RUTA_PREVISUALIZACION)

@Composable
fun AppAdbrand() {
    val navegador = rememberNavController()
    val inicio = remember { if (GestorSesion.haySesion()) RUTA_BIENVENIDA else RUTA_LOGIN }
    val entrada by navegador.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val destinos = remember {
        listOf(
            ItemNavegacion(RUTA_EMPRESA, "Empresa", Icons.Filled.Home),
            ItemNavegacion(RUTA_PERFIL, "Perfil", Icons.Filled.Person),
            ItemNavegacion(RUTA_PREVISUALIZACION, "Vista", Icons.Filled.Visibility)
        )
    }

    Scaffold(
        bottomBar = {
            if (rutaActual in DESTINOS_PRINCIPALES) {
                BarraInferior(rutaActual, destinos, navegador::navegarA)
            }
        }
    ) { relleno ->
        NavHost(navegador, startDestination = inicio, modifier = Modifier.padding(relleno)) {
            composable(RUTA_LOGIN) {
                LoginScreen(
                    alAutenticarse = { navegador.navigate(RUTA_BIENVENIDA) { popUpTo(RUTA_LOGIN) { inclusive = true } } },
                    irARegistro = { navegador.navigate(RUTA_REGISTRO) },
                    irARecuperar = { navegador.navigate(RUTA_RECUPERAR) }
                )
            }
            composable(RUTA_REGISTRO) { RegistroScreen(alVolver = { navegador.popBackStack() }) }
            composable(RUTA_RECUPERAR) { RecuperarContrasenaScreen(alVolver = { navegador.popBackStack() }) }
            composable(RUTA_BIENVENIDA) {
                BienvenidaScreen {
                    navegador.navigate(RUTA_EMPRESA) { popUpTo(RUTA_BIENVENIDA) { inclusive = true } }
                }
            }
            composable(RUTA_EMPRESA) {
                DatosEmpresaScreen(alCerrarSesion = {
                    navegador.navigate(RUTA_LOGIN) { popUpTo(RUTA_EMPRESA) { inclusive = true } }
                })
            }
            composable(RUTA_PERFIL) { PerfilNegocioScreen() }
            composable(RUTA_PREVISUALIZACION) { PrevisualizacionScreen() }
        }
    }
}

private fun androidx.navigation.NavHostController.navegarA(ruta: String) {
    navigate(ruta) {
        popUpTo(RUTA_EMPRESA) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
