package com.adbrand.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adbrand.core.session.GestorSesion
import com.adbrand.ui.auth.screens.LoginScreen
import com.adbrand.ui.perfil.screens.DatosEmpresaScreen
import com.adbrand.ui.perfil.screens.PerfilNegocioScreen
import com.adbrand.ui.previsualizacion.screens.PrevisualizacionScreen

private const val RUTA_LOGIN = "login"
private const val RUTA_EMPRESA = "empresa"
private const val RUTA_PERFIL = "perfil"
private const val RUTA_PREVISUALIZACION = "previsualizacion"

// HU 42-45: navegación principal de la app.
@Composable
fun AppAdbrand() {
    val navegador = rememberNavController()
    val inicio = remember { if (GestorSesion.haySesion()) RUTA_EMPRESA else RUTA_LOGIN }
    val entrada by navegador.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    Scaffold(
        bottomBar = {
            if (rutaActual != null && rutaActual != RUTA_LOGIN) {
                NavigationBar {
                    NavigationBarItem(
                        selected = rutaActual == RUTA_EMPRESA,
                        onClick = { navegador.navegarA(RUTA_EMPRESA) },
                        icon = { Text("E") },
                        label = { Text("Empresa") }
                    )
                    NavigationBarItem(
                        selected = rutaActual == RUTA_PERFIL,
                        onClick = { navegador.navegarA(RUTA_PERFIL) },
                        icon = { Text("P") },
                        label = { Text("Perfil") }
                    )
                    NavigationBarItem(
                        selected = rutaActual == RUTA_PREVISUALIZACION,
                        onClick = { navegador.navegarA(RUTA_PREVISUALIZACION) },
                        icon = { Text("V") },
                        label = { Text("Vista previa") }
                    )
                }
            }
        }
    ) { relleno ->
        NavHost(
            navController = navegador,
            startDestination = inicio,
            modifier = Modifier.padding(relleno)
        ) {
            composable(RUTA_LOGIN) {
                LoginScreen(
                    alAutenticarse = {
                        navegador.navigate(RUTA_EMPRESA) {
                            popUpTo(RUTA_LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(RUTA_EMPRESA) {
                DatosEmpresaScreen(
                    alCerrarSesion = {
                        navegador.navigate(RUTA_LOGIN) {
                            popUpTo(RUTA_EMPRESA) { inclusive = true }
                        }
                    }
                )
            }
            composable(RUTA_PERFIL) {
                PerfilNegocioScreen()
            }
            composable(RUTA_PREVISUALIZACION) {
                PrevisualizacionScreen()
            }
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
