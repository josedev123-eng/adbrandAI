package com.adbrand.ui.plan

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PlanRepository
import com.adbrand.domain.model.MiSuscripcion
import com.adbrand.domain.model.PlanDisponible
import com.adbrand.ui.plan.viewmodel.PlanViewModel
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

// HU 52: la pantalla del plan carga la suscripción y el catálogo, y permite
// reintentar cuando el servidor no responde.
@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class PlanFalso(
        var suscripcion: Resultado<MiSuscripcion>,
        var planes: Resultado<List<PlanDisponible>>
    ) : PlanRepository {

        override suspend fun mia(): Resultado<MiSuscripcion> = suscripcion

        override suspend fun planes(): Resultado<List<PlanDisponible>> = planes
    }

    @Test
    fun muestraElPlanYElCatalogo() = runTest {
        val viewModel = PlanViewModel(
            PlanFalso(
                Resultado.Exito(MiSuscripcion(plan = "Emprende", estado = "ACTIVA", diasRestantes = 3)),
                Resultado.Exito(listOf(PlanDisponible(id = 1, nombre = "Emprende")))
            )
        )

        viewModel.cargar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertTrue(estado.cargado)
        assertNull(estado.error)
        assertEquals("Emprende", estado.suscripcion.plan)
        assertEquals(1, estado.planes.size)
    }

    @Test
    fun sinPlanMuestraSoloElCatalogo() = runTest {
        val viewModel = PlanViewModel(
            PlanFalso(
                Resultado.Exito(MiSuscripcion()),
                Resultado.Exito(listOf(PlanDisponible(id = 1, nombre = "Emprende")))
            )
        )

        viewModel.cargar()
        advanceUntilIdle()

        assertFalse(viewModel.estado.value.suscripcion.tienePlan)
        assertEquals(1, viewModel.estado.value.planes.size)
    }

    @Test
    fun unErrorSePuedeReintentar() = runTest {
        val repositorio = PlanFalso(
            Resultado.Error("Sin conexión."),
            Resultado.Exito(emptyList())
        )
        val viewModel = PlanViewModel(repositorio)

        viewModel.cargar()
        advanceUntilIdle()
        assertEquals("Sin conexión.", viewModel.estado.value.error)

        repositorio.suscripcion = Resultado.Exito(
            MiSuscripcion(plan = "Emprende", estado = "ACTIVA", diasRestantes = 10)
        )
        viewModel.cargar()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.error)
        assertEquals("Emprende", viewModel.estado.value.suscripcion.plan)
    }
}
