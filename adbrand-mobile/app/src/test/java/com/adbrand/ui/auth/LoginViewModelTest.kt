package com.adbrand.ui.auth

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AuthRepository
import com.adbrand.domain.model.Usuario
import com.adbrand.ui.auth.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 42: pruebas del LoginViewModel con un repositorio falso, sin red ni Android.
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class RepositorioFalso(
        private val resultado: Resultado<Usuario>
    ) : AuthRepository {
        override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario> =
            resultado

        override suspend fun cerrarSesion(): Resultado<Unit> = Resultado.Exito(Unit)
    }

    private fun usuario() = Usuario(1L, "Luis Rojas", "Bodega San Martín", "luis@adbrand.pe")

    @Test
    fun credencialesCorrectasDejanLaSesionAutenticada() = runTest {
        val viewModel = LoginViewModel(RepositorioFalso(Resultado.Exito(usuario())))
        viewModel.actualizarCorreo("luis@adbrand.pe")
        viewModel.actualizarContrasena("Prueba123")

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.autenticado)
        assertFalse(viewModel.estado.value.cargando)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun un401MuestraElMensajeDeCredencialesIncorrectas() = runTest {
        val viewModel = LoginViewModel(
            RepositorioFalso(Resultado.Error("Correo o contraseña incorrectos.", 401))
        )
        viewModel.actualizarCorreo("luis@adbrand.pe")
        viewModel.actualizarContrasena("Equivocada1")

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertEquals("Correo o contraseña incorrectos.", viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.autenticado)
    }

    @Test
    fun losCamposVaciosSeValidanAntesDeLlamarAlBackend() = runTest {
        val viewModel = LoginViewModel(RepositorioFalso(Resultado.Exito(usuario())))

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertEquals("Ingresa tu correo.", viewModel.estado.value.errorCorreo)
        assertEquals("Ingresa tu contraseña.", viewModel.estado.value.errorContrasena)
        assertFalse(viewModel.estado.value.autenticado)
    }
}
