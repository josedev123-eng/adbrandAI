package com.adbrand.ui.panel

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.InformeRepository
import com.adbrand.domain.model.CampanaInforme
import com.adbrand.domain.model.InformeCampanas
import com.adbrand.domain.model.RedSocial
import com.adbrand.ui.panel.viewmodel.PanelViewModel
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

// HU 37: panel de consulta de campañas, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class PanelViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class InformeFalso(
        private val resultado: Resultado<InformeCampanas>
    ) : InformeRepository {
        var llamadas = 0

        override suspend fun obtener(): Resultado<InformeCampanas> {
            llamadas++
            return resultado
        }
    }

    private fun informe() = InformeCampanas(
        empresa = "Bodega San Martín",
        total = 3,
        aprobados = 1,
        dudosos = 1,
        rechazados = 1,
        campanas = listOf(
            CampanaInforme(1L, "2026-11-01T10:00:00", RedSocial.INSTAGRAM, "Panetones", "APROBADO"),
            CampanaInforme(2L, "2026-11-02T11:00:00", RedSocial.FACEBOOK, "Sorteo", "DUDOSO"),
            CampanaInforme(3L, "2026-11-03T12:00:00", RedSocial.INSTAGRAM, "Promo", "RECHAZADO")
        )
    )

    @Test
    fun cargarTraeElInformeYLosTotales() = runTest {
        val viewModel = PanelViewModel(InformeFalso(Resultado.Exito(informe())))

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("Bodega San Martín", viewModel.estado.value.informe?.empresa)
        assertEquals(3, viewModel.estado.value.informe?.total)
        assertEquals(3, viewModel.estado.value.campanasVisibles.size)
        assertTrue(viewModel.estado.value.cargado)
        assertFalse(viewModel.estado.value.cargando)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun elFiltroLocalDejaSoloLasCampanasDelEstado() = runTest {
        val viewModel = PanelViewModel(InformeFalso(Resultado.Exito(informe())))
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.filtrar("DUDOSO")

        assertEquals(1, viewModel.estado.value.campanasVisibles.size)
        assertEquals("Sorteo", viewModel.estado.value.campanasVisibles.first().oferta)

        viewModel.filtrar(null)

        assertEquals(3, viewModel.estado.value.campanasVisibles.size)
    }

    @Test
    fun unFiltroSinResultadosDevuelveListaVacia() = runTest {
        val viewModel = PanelViewModel(InformeFalso(Resultado.Exito(informe())))
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.filtrar("PENDIENTE")

        assertTrue(viewModel.estado.value.campanasVisibles.isEmpty())
        assertEquals(3, viewModel.estado.value.informe?.campanas?.size)
    }

    @Test
    fun unErrorSeMuestraEnElPanel() = runTest {
        val viewModel = PanelViewModel(
            InformeFalso(Resultado.Error("No se pudo cargar tu panel.", 500))
        )

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("No se pudo cargar tu panel.", viewModel.estado.value.error)
        assertNull(viewModel.estado.value.informe)
        assertTrue(viewModel.estado.value.cargado)
    }
}
