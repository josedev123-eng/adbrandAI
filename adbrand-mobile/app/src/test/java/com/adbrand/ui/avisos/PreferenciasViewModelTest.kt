package com.adbrand.ui.avisos

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PreferenciasRepository
import com.adbrand.domain.model.CampoPreferencia
import com.adbrand.domain.model.PreferenciasAvisos
import com.adbrand.ui.avisos.viewmodel.PreferenciasViewModel
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

// HU 51: el interruptor se pinta al instante, se confirma con el servidor y
// vuelve solo si el guardado falla.
@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class PreferenciasFalsas(
        var lectura: Resultado<PreferenciasAvisos>,
        var escritura: Resultado<PreferenciasAvisos>
    ) : PreferenciasRepository {

        var campoEnviado: CampoPreferencia? = null
        var valorEnviado: Boolean? = null

        override suspend fun obtener(): Resultado<PreferenciasAvisos> = lectura

        override suspend fun actualizar(
            campo: CampoPreferencia,
            valor: Boolean
        ): Resultado<PreferenciasAvisos> {
            campoEnviado = campo
            valorEnviado = valor
            return escritura
        }
    }

    @Test
    fun elInterruptorSePintaAntesDeGuardar() = runTest {
        val repositorio = PreferenciasFalsas(
            Resultado.Exito(PreferenciasAvisos()),
            Resultado.Exito(PreferenciasAvisos(campanas = false, todasActivas = false))
        )
        val viewModel = PreferenciasViewModel(repositorio)

        viewModel.alternar(CampoPreferencia.CAMPANAS, false)

        assertFalse(viewModel.estado.value.preferencias.campanas)
        assertTrue(viewModel.estado.value.guardando)

        advanceUntilIdle()

        assertEquals(CampoPreferencia.CAMPANAS, repositorio.campoEnviado)
        assertEquals(false, repositorio.valorEnviado)
        assertFalse(viewModel.estado.value.guardando)
        assertEquals("Preferencia guardada.", viewModel.estado.value.mensaje)
    }

    @Test
    fun siFallaElGuardadoVuelveAlValorAnterior() = runTest {
        val viewModel = PreferenciasViewModel(
            PreferenciasFalsas(
                Resultado.Exito(PreferenciasAvisos()),
                Resultado.Error("No se pudo guardar el cambio.")
            )
        )

        viewModel.alternar(CampoPreferencia.SUSCRIPCIONES, false)
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.preferencias.suscripciones)
        assertEquals("No se pudo guardar el cambio.", viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.guardando)
    }

    @Test
    fun cargaLasPreferenciasDelServidor() = runTest {
        val viewModel = PreferenciasViewModel(
            PreferenciasFalsas(
                Resultado.Exito(
                    PreferenciasAvisos(campanas = true, suscripciones = false, novedades = true)
                ),
                Resultado.Exito(PreferenciasAvisos())
            )
        )

        viewModel.cargar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.cargado)
        assertFalse(viewModel.estado.value.preferencias.suscripciones)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun unErrorDeCargaSePuedeReintentar() = runTest {
        val repositorio = PreferenciasFalsas(
            Resultado.Error("Sin conexión."),
            Resultado.Exito(PreferenciasAvisos())
        )
        val viewModel = PreferenciasViewModel(repositorio)

        viewModel.cargar()
        advanceUntilIdle()
        assertEquals("Sin conexión.", viewModel.estado.value.error)

        repositorio.lectura = Resultado.Exito(PreferenciasAvisos(novedades = false))
        viewModel.reintentar()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.preferencias.novedades)
    }
}
