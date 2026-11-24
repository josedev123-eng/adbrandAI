package com.adbrand.ui.metricas

import com.adbrand.core.network.Resultado
import com.adbrand.data.remote.MetricasApi
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.OpcionPeriodo
import com.adbrand.domain.model.PeriodoMetricas
import com.adbrand.domain.model.RangoFechas
import com.adbrand.ui.metricas.viewmodel.MetricasViewModel
import java.time.LocalDate
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 60: filtrar las métricas por periodo, con fechas válidas e inválidas, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class MetricasPeriodoViewModelTest {

    private val despachador = StandardTestDispatcher()

    // Un día fijo, así la prueba no cambia con la fecha real.
    private val hoy = LocalDate.of(2026, 11, 23)

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    // Guarda con qué periodo se pidió cada métrica.
    private class RepositorioFalso : MetricasRepository {
        val pedidos = mutableListOf<RangoFechas>()
        override suspend fun contenido(rango: RangoFechas): Resultado<MetricasContenido> {
            pedidos.add(rango)
            return Resultado.Exito(MetricasContenido())
        }

        override suspend fun publicaciones(rango: RangoFechas): Resultado<MetricasPublicaciones> =
            Resultado.Exito(MetricasPublicaciones())
    }

    @Test
    fun esteMesYUltimos3MesesCalculanSusFechas() {
        assertEquals(RangoFechas("2026-11-01", "2026-11-30"), PeriodoMetricas.rango(OpcionPeriodo.ESTE_MES, hoy))
        assertEquals(RangoFechas("2026-09-01", "2026-11-30"), PeriodoMetricas.rango(OpcionPeriodo.ULTIMOS_3_MESES, hoy))
        assertEquals(RangoFechas(null, null), PeriodoMetricas.rango(OpcionPeriodo.TODO, hoy))
        // En enero, los últimos 3 meses empiezan en noviembre del año anterior.
        assertEquals(
            RangoFechas("2026-11-01", "2027-01-31"),
            PeriodoMetricas.rango(OpcionPeriodo.ULTIMOS_3_MESES, LocalDate.of(2027, 1, 10))
        )
    }

    @Test
    fun alElegirUnaOpcionSeVuelvenAPedirLasMetricasConEsasFechas() = runTest(despachador) {
        val repositorio = RepositorioFalso()
        val viewModel = MetricasViewModel(repositorio) { hoy }

        viewModel.elegirPeriodo(OpcionPeriodo.ESTE_MES)
        advanceUntilIdle()
        viewModel.elegirPeriodo(OpcionPeriodo.ULTIMOS_3_MESES)
        advanceUntilIdle()

        assertEquals(
            listOf(RangoFechas("2026-11-01", "2026-11-30"), RangoFechas("2026-09-01", "2026-11-30")),
            repositorio.pedidos
        )
    }

    @Test
    fun rangoPropioValidoSeAplicaAlTocarAplicar() = runTest(despachador) {
        val repositorio = RepositorioFalso()
        val viewModel = MetricasViewModel(repositorio) { hoy }

        viewModel.elegirPeriodo(OpcionPeriodo.RANGO)
        viewModel.cambiarDesde("2026-10-01")
        viewModel.cambiarHasta("2026-10-31")
        advanceUntilIdle()
        // Elegir "Rango propio" solo muestra los campos: todavía no se consulta.
        assertTrue(repositorio.pedidos.isEmpty())

        viewModel.aplicarRango()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.avisoPeriodo)
        assertEquals(listOf(RangoFechas("2026-10-01", "2026-10-31")), repositorio.pedidos)
    }

    @Test
    fun conElInicioDespuesDelFinAvisaYNoConsulta() = runTest(despachador) {
        val repositorio = RepositorioFalso()
        val viewModel = MetricasViewModel(repositorio) { hoy }

        viewModel.elegirPeriodo(OpcionPeriodo.RANGO)
        viewModel.cambiarDesde("2026-11-30")
        viewModel.cambiarHasta("2026-11-01")
        viewModel.aplicarRango()
        advanceUntilIdle()

        assertEquals("La fecha de inicio no puede ser posterior a la fecha de fin.", viewModel.estado.value.avisoPeriodo)
        assertTrue(repositorio.pedidos.isEmpty())
    }

    @Test
    fun conUnaFechaMalEscritaAvisaYNoConsulta() = runTest(despachador) {
        val repositorio = RepositorioFalso()
        val viewModel = MetricasViewModel(repositorio) { hoy }

        viewModel.elegirPeriodo(OpcionPeriodo.RANGO)
        viewModel.cambiarDesde("noviembre")
        viewModel.aplicarRango()
        advanceUntilIdle()

        assertEquals("Escribe las fechas como AAAA-MM-DD.", viewModel.estado.value.avisoPeriodo)
        assertTrue(repositorio.pedidos.isEmpty())
    }

    @Test
    fun laConsultaSoloLlevaLasFechasQueHay() {
        assertEquals("", MetricasApi.consulta(RangoFechas(null, null)))
        assertEquals("?desde=2026-11-01", MetricasApi.consulta(RangoFechas("2026-11-01", null)))
        assertEquals("?desde=2026-11-01&hasta=2026-11-30", MetricasApi.consulta(RangoFechas("2026-11-01", "2026-11-30")))
    }
}
