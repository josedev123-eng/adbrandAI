package com.adbrand.ui.metricas

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.domain.model.ContenidoPorMes
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.ui.metricas.screens.etiquetaMes
import com.adbrand.ui.metricas.viewmodel.MetricasViewModel
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

// HU 57: métricas del contenido con anuncios, sin anuncios y con error, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class MetricasViewModelTest {

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
        private val resultado: Resultado<MetricasContenido>,
        private val publicaciones: Resultado<MetricasPublicaciones> = Resultado.Exito(MetricasPublicaciones())
    ) : MetricasRepository {
        var llamadas = 0
        override suspend fun contenido(): Resultado<MetricasContenido> {
            llamadas++
            return resultado
        }

        override suspend fun publicaciones(): Resultado<MetricasPublicaciones> = publicaciones
    }

    @Test
    fun conAnunciosMuestraLosTotalesPorEstadoYPorMes() = runTest(despachador) {
        val metricas = MetricasContenido(
            total = 4, aprobados = 2, enRevision = 1, rechazados = 1,
            porMes = listOf(ContenidoPorMes("2026-10", 1), ContenidoPorMes("2026-11", 3))
        )
        val viewModel = MetricasViewModel(RepositorioFalso(Resultado.Exito(metricas)))

        viewModel.cargar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.cargado)
        assertFalse(estado.cargando)
        assertNull(estado.error)
        assertEquals(4L, estado.metricas.total)
        assertEquals(2L, estado.metricas.aprobados)
        assertEquals(1L, estado.metricas.enRevision)
        assertEquals(1L, estado.metricas.rechazados)
        assertEquals(2, estado.metricas.porMes.size)
        assertFalse(estado.metricas.sinContenido)
    }

    @Test
    fun sinAnunciosLosTotalesQuedanEnCeroYSeMarcaParaElAviso() = runTest(despachador) {
        val viewModel = MetricasViewModel(RepositorioFalso(Resultado.Exito(MetricasContenido())))

        viewModel.cargar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.cargado)
        assertEquals(0L, estado.metricas.total)
        assertEquals(0L, estado.metricas.aprobados)
        assertTrue(estado.metricas.sinContenido)
    }

    @Test
    fun conErrorMuestraElMensajeYDejaReintentar() = runTest(despachador) {
        val repositorio = RepositorioFalso(Resultado.Error("No se pudo conectar con el servidor. Revisa tu conexión."))
        val viewModel = MetricasViewModel(repositorio)

        viewModel.cargar()
        advanceUntilIdle()
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión.", viewModel.estado.value.error)

        viewModel.cargar()
        advanceUntilIdle()
        assertEquals(2, repositorio.llamadas)
    }

    @Test
    fun etiquetaMesMuestraElMesYElAnio() {
        assertEquals("Nov 2026", etiquetaMes("2026-11"))
        assertEquals("Ene 2027", etiquetaMes("2027-01"))
        assertEquals("sin-fecha", etiquetaMes("sin-fecha"))
    }
}
