package com.adbrand.ui.metricas

import androidx.compose.ui.graphics.Color
import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.ProximaPublicacion
import com.adbrand.domain.model.PublicacionesPorRed
import com.adbrand.ui.metricas.screens.colorDesdeHex
import com.adbrand.ui.metricas.screens.fechaCorta
import com.adbrand.ui.metricas.screens.nombreRed
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// HU 58: métricas de publicaciones en el ViewModel y los textos de la pantalla, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class MetricasPublicacionesViewModelTest {

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
        private val publicaciones: Resultado<MetricasPublicaciones>
    ) : MetricasRepository {
        override suspend fun contenido(): Resultado<MetricasContenido> = Resultado.Exito(MetricasContenido())
        override suspend fun publicaciones(): Resultado<MetricasPublicaciones> = publicaciones
    }

    @Test
    fun cargaLasPublicacionesPorRedYLasProximas() = runTest(despachador) {
        val datos = MetricasPublicaciones(
            programadas = 3, publicadas = 1, canceladas = 1,
            porRed = listOf(PublicacionesPorRed("INSTAGRAM", 2, 1), PublicacionesPorRed("FACEBOOK", 1, 0)),
            proximas = listOf(
                ProximaPublicacion(5, "Promo 2", "INSTAGRAM", "2026-12-05", "18:00:00"),
                ProximaPublicacion(4, "Promo 1", "INSTAGRAM", "2026-12-10", "18:00:00", "Navidad", "#C62828")
            )
        )
        val viewModel = MetricasViewModel(RepositorioFalso(Resultado.Exito(datos)))

        viewModel.cargar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertNull(estado.error)
        assertEquals(3L, estado.publicaciones.programadas)
        assertEquals(1L, estado.publicaciones.publicadas)
        assertEquals(2, estado.publicaciones.porRed.size)
        assertEquals("Promo 2", estado.publicaciones.proximas.first().titulo)
        assertEquals("Navidad", estado.publicaciones.proximas[1].campanaNombre)
    }

    @Test
    fun siFallanLasPublicacionesMuestraElMensaje() = runTest(despachador) {
        val viewModel = MetricasViewModel(RepositorioFalso(Resultado.Error("No se pudieron cargar las métricas. Intenta de nuevo.")))

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("No se pudieron cargar las métricas. Intenta de nuevo.", viewModel.estado.value.error)
    }

    @Test
    fun textosDeLaPantalla() {
        assertEquals("25 Nov", fechaCorta("2026-11-25"))
        assertEquals("1 Ene", fechaCorta("2027-01-01"))
        assertEquals("sin-fecha", fechaCorta("sin-fecha"))

        assertEquals("Instagram", nombreRed("INSTAGRAM"))
        assertEquals("Facebook", nombreRed("FACEBOOK"))

        assertEquals(Color(0xFFC62828), colorDesdeHex("#C62828"))
        assertNull(colorDesdeHex("rojo"))
        assertNull(colorDesdeHex(null))
    }
}
