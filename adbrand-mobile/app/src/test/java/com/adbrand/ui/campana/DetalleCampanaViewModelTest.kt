package com.adbrand.ui.campana

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.CampanaRepository
import com.adbrand.domain.model.Campana
import com.adbrand.ui.campana.viewmodel.DetalleCampanaViewModel
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

// HU 49: la ficha abierta desde el aviso carga la campaña, distingue la que ya
// no existe y permite reintentar cuando falla la red.
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleCampanaViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class CampanaFalsa(var resultado: Resultado<Campana>) : CampanaRepository {
        var idPedido: Long? = null

        override suspend fun obtener(id: Long): Resultado<Campana> {
            idPedido = id
            return resultado
        }
    }

    @Test
    fun abreLaCampanaDelAviso() = runTest {
        val repositorio = CampanaFalsa(Resultado.Exito(campana()))
        val viewModel = DetalleCampanaViewModel(repositorio)

        viewModel.cargar(12L)
        advanceUntilIdle()

        assertEquals(12L, repositorio.idPedido)
        assertEquals("Navidad 2026", viewModel.estado.value.campana?.nombre)
        assertNull(viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun unaCampanaInexistenteMuestraElEstadoVacio() = runTest {
        val viewModel = DetalleCampanaViewModel(
            CampanaFalsa(
                Resultado.Error(
                    mensaje = "La campaña ya no existe o no es de tu empresa.",
                    codigo = 404,
                    codigoError = "CAMPANA_NO_ENCONTRADA"
                )
            )
        )

        viewModel.cargar(99L)
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.noEncontrada)
        assertNull(viewModel.estado.value.campana)
        assertEquals(
            "La campaña ya no existe o no es de tu empresa.",
            viewModel.estado.value.error
        )
    }

    @Test
    fun unAvisoSinIdentificadorNoConsultaAlBackend() = runTest {
        val repositorio = CampanaFalsa(Resultado.Exito(campana()))
        val viewModel = DetalleCampanaViewModel(repositorio)

        viewModel.cargar(0L)
        advanceUntilIdle()

        assertNull(repositorio.idPedido)
        assertTrue(viewModel.estado.value.noEncontrada)
    }

    @Test
    fun unErrorDeRedSePuedeReintentar() = runTest {
        val repositorio = CampanaFalsa(Resultado.Error("Sin conexión."))
        val viewModel = DetalleCampanaViewModel(repositorio)

        viewModel.cargar(12L)
        advanceUntilIdle()
        assertEquals("Sin conexión.", viewModel.estado.value.error)

        repositorio.resultado = Resultado.Exito(campana())
        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals("Navidad 2026", viewModel.estado.value.campana?.nombre)
        assertNull(viewModel.estado.value.error)
    }

    private companion object {
        fun campana() = Campana(
            id = 12L,
            nombre = "Navidad 2026",
            color = "#C62828",
            tipo = "CAMPANA",
            fechaInicio = "2026-12-01",
            fechaFin = "2026-12-25"
        )
    }
}
