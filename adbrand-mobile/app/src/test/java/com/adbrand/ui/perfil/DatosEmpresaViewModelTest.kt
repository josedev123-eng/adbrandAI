package com.adbrand.ui.perfil

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AuthRepository
import com.adbrand.data.repository.CuentaRepository
import com.adbrand.domain.model.Empresa
import com.adbrand.ui.perfil.viewmodel.DatosEmpresaViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 41: validación de RUC/teléfono, guardado y cierre de sesión sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class DatosEmpresaViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class CuentaFalsa(
        private val empresa: Empresa = Empresa(
            nombreEmpresa = "Bodega San Martín",
            correo = "luis@adbrand.pe",
            ruc = "20123456789",
            razonSocial = "Bodega San Martín SAC",
            telefono = "987654321",
            direccion = "Av. Lima 123"
        )
    ) : CuentaRepository {
        var guardada: Empresa? = null

        override suspend fun obtener(): Resultado<Empresa> = Resultado.Exito(empresa)

        override suspend fun guardar(empresa: Empresa): Resultado<Empresa> {
            guardada = empresa
            return Resultado.Exito(empresa)
        }
    }

    private class AuthFalsa : AuthRepository {
        var cerrada = false

        override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<com.adbrand.domain.model.Usuario> =
            Resultado.Error("No usado")

        override suspend fun cerrarSesion(): Resultado<Unit> {
            cerrada = true
            return Resultado.Exito(Unit)
        }
    }

    @Test
    fun cargarTraeLosDatosDeLaEmpresa() = runTest {
        val viewModel = DatosEmpresaViewModel(CuentaFalsa(), AuthFalsa())

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("Bodega San Martín", viewModel.estado.value.nombreEmpresa)
        assertEquals("20123456789", viewModel.estado.value.ruc)
        assertEquals("987654321", viewModel.estado.value.telefono)
    }

    @Test
    fun unRucInvalidoNoSeGuarda() = runTest {
        val cuenta = CuentaFalsa()
        val viewModel = DatosEmpresaViewModel(cuenta, AuthFalsa())
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.actualizarRuc("123")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals("El RUC tiene 11 dígitos y empieza en 10 o 20.", viewModel.estado.value.errorRuc)
        assertEquals(null, cuenta.guardada)
    }

    @Test
    fun unTelefonoInvalidoNoSeGuarda() = runTest {
        val cuenta = CuentaFalsa()
        val viewModel = DatosEmpresaViewModel(cuenta, AuthFalsa())
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.actualizarTelefono("12345")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(
            "Escribe un celular de 9 dígitos que empiece en 9, o un fijo de 7 dígitos.",
            viewModel.estado.value.errorTelefono
        )
        assertEquals(null, cuenta.guardada)
    }

    @Test
    fun guardarConDatosValidosMuestraElMensaje() = runTest {
        val cuenta = CuentaFalsa()
        val viewModel = DatosEmpresaViewModel(cuenta, AuthFalsa())
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals("Datos guardados.", viewModel.estado.value.mensaje)
        assertEquals("20123456789", cuenta.guardada?.ruc)
    }

    @Test
    fun cerrarSesionAvisaALaPantalla() = runTest {
        val auth = AuthFalsa()
        val viewModel = DatosEmpresaViewModel(CuentaFalsa(), auth)

        viewModel.cerrarSesion()
        advanceUntilIdle()

        assertTrue(auth.cerrada)
        assertTrue(viewModel.estado.value.sesionCerrada)
        assertFalse(viewModel.estado.value.cargando)
    }
}
