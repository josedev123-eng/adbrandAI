package com.adbrand.ui.perfil

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.CuentaRepository
import com.adbrand.data.repository.PerfilRepository
import com.adbrand.domain.model.Empresa
import com.adbrand.domain.model.PerfilNegocio
import com.adbrand.domain.model.Tono
import com.adbrand.ui.perfil.viewmodel.PerfilNegocioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 44: perfil del negocio con datos de contacto, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilNegocioViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class PerfilFalso(
        private val perfil: PerfilNegocio = PerfilNegocio(
            id = 1L,
            nombreComercial = "Bodega San Martín",
            rubro = "Comercio",
            publicoObjetivo = "Familias del barrio",
            tono = Tono.CERCANO,
            descripcion = "Atención de barrio"
        )
    ) : PerfilRepository {
        var guardado: PerfilNegocio? = null

        override suspend fun obtener(): Resultado<PerfilNegocio> = Resultado.Exito(perfil)

        override suspend fun guardar(perfil: PerfilNegocio): Resultado<PerfilNegocio> {
            guardado = perfil
            return Resultado.Exito(perfil)
        }
    }

    private class CuentaFalsa : CuentaRepository {
        var guardada: Empresa? = null

        override suspend fun obtener(): Resultado<Empresa> = Resultado.Exito(
            Empresa(
                nombreEmpresa = "Bodega San Martín",
                correo = "luis@adbrand.pe",
                ruc = "20123456789",
                razonSocial = "Bodega San Martín SAC",
                telefono = "987654321",
                direccion = "Av. Lima 123"
            )
        )

        override suspend fun guardar(empresa: Empresa): Resultado<Empresa> {
            guardada = empresa
            return Resultado.Exito(empresa)
        }
    }

    @Test
    fun cargarTraePerfilYContacto() = runTest {
        val viewModel = PerfilNegocioViewModel(PerfilFalso(), CuentaFalsa())

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("Bodega San Martín", viewModel.estado.value.nombreComercial)
        assertEquals(Tono.CERCANO, viewModel.estado.value.tono)
        assertEquals("luis@adbrand.pe", viewModel.estado.value.correo)
        assertEquals("987654321", viewModel.estado.value.telefono)
    }

    @Test
    fun losCamposObligatoriosSeValidanAntesDeGuardar() = runTest {
        val perfiles = PerfilFalso()
        val viewModel = PerfilNegocioViewModel(perfiles, CuentaFalsa())
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.actualizarNombreComercial("")
        viewModel.actualizarRubro("")
        viewModel.actualizarPublicoObjetivo("")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals("Ingresa el nombre comercial.", viewModel.estado.value.errorNombre)
        assertEquals("Selecciona el rubro.", viewModel.estado.value.errorRubro)
        assertEquals("Describe a tu público objetivo.", viewModel.estado.value.errorPublico)
        assertEquals(null, perfiles.guardado)
    }

    @Test
    fun guardarConDatosValidosActualizaPerfilYTelefono() = runTest {
        val perfiles = PerfilFalso()
        val cuentas = CuentaFalsa()
        val viewModel = PerfilNegocioViewModel(perfiles, cuentas)
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.actualizarTono(Tono.PROFESIONAL)
        viewModel.actualizarTelefono("912345678")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(Tono.PROFESIONAL, perfiles.guardado?.tono)
        assertEquals("912345678", cuentas.guardada?.telefono)
        assertEquals("Perfil guardado.", viewModel.estado.value.mensaje)
        assertTrue(viewModel.estado.value.error == null)
    }
}
