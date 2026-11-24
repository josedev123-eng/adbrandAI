package com.adbrand.ui.metricas

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.GuardadoPdf
import com.adbrand.data.repository.MetricasRepository
import com.adbrand.data.repository.ReportesRepository
import com.adbrand.domain.model.MetricasContenido
import com.adbrand.domain.model.MetricasPublicaciones
import com.adbrand.domain.model.OpcionPeriodo
import com.adbrand.domain.model.RangoFechas
import com.adbrand.domain.model.ReporteDescargado
import com.adbrand.ui.metricas.viewmodel.MetricasViewModel
import java.io.File
import java.io.IOException
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// HU 59: descarga del reporte en PDF, con datos y sin conexión, sin usar la red real.
@OptIn(ExperimentalCoroutinesApi::class)
class ReporteDescargaTest {

    private val despachador = StandardTestDispatcher()
    private val hoy = LocalDate.of(2026, 11, 24)

    // Carpeta temporal que JUnit borra al terminar cada prueba.
    @get:Rule
    val carpetas = TemporaryFolder()

    private val pdfDePrueba = "%PDF-1.4\ninforme de campañas".toByteArray()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class MetricasFalsas : MetricasRepository {
        override suspend fun contenido(rango: RangoFechas): Resultado<MetricasContenido> =
            Resultado.Exito(MetricasContenido())

        override suspend fun publicaciones(rango: RangoFechas): Resultado<MetricasPublicaciones> =
            Resultado.Exito(MetricasPublicaciones())
    }

    // Hace lo mismo que el repositorio real, pero los bytes y la conexión los decide la prueba.
    private class ReportesFalsos(var hayConexion: Boolean, var bytes: ByteArray) : ReportesRepository {
        val pedidos = mutableListOf<RangoFechas>()

        override suspend fun descargarPdf(rango: RangoFechas, carpeta: File): Resultado<ReporteDescargado> {
            pedidos.add(rango)
            if (!hayConexion) return Resultado.Error("No se pudo conectar con el servidor. Revisa tu conexión.")
            val nombre = "informe-campanas-2026-11-24.pdf"
            return try {
                Resultado.Exito(ReporteDescargado(nombre, GuardadoPdf.guardar(bytes, carpeta, nombre)))
            } catch (error: IOException) {
                Resultado.Error("No se pudo guardar el PDF en el celular.")
            }
        }
    }

    private fun archivosEn(carpeta: File): List<String> = carpeta.listFiles()?.map { it.name } ?: emptyList()

    @Test
    fun conDatosGuardaElPdfConElPeriodoElegido() = runTest(despachador) {
        val reportes = ReportesFalsos(hayConexion = true, bytes = pdfDePrueba)
        val viewModel = MetricasViewModel(MetricasFalsas(), reportes) { hoy }
        val carpeta = carpetas.newFolder("documentos")

        viewModel.elegirPeriodo(OpcionPeriodo.ESTE_MES)
        advanceUntilIdle()
        viewModel.descargarPdf(carpeta)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.descargando)
        assertNull(estado.errorDescarga)
        assertNotNull(estado.reporte)
        assertEquals("informe-campanas-2026-11-24.pdf", estado.reporte!!.nombre)
        assertTrue(estado.reporte!!.archivo.readBytes().contentEquals(pdfDePrueba))
        assertEquals(RangoFechas("2026-11-01", "2026-11-30"), reportes.pedidos.last())
        assertEquals(listOf("informe-campanas-2026-11-24.pdf"), archivosEn(carpeta))
    }

    @Test
    fun sinConexionMuestraElMensajeYNoCreaArchivo() = runTest(despachador) {
        val viewModel = MetricasViewModel(MetricasFalsas(), ReportesFalsos(hayConexion = false, bytes = pdfDePrueba)) { hoy }
        val carpeta = carpetas.newFolder("documentos")

        viewModel.descargarPdf(carpeta)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.descargando)
        assertNull(estado.reporte)
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión.", estado.errorDescarga)
        assertTrue(archivosEn(carpeta).isEmpty())
    }

    @Test
    fun alVolverLaConexionSePuedeDescargarDeNuevo() = runTest(despachador) {
        val reportes = ReportesFalsos(hayConexion = false, bytes = pdfDePrueba)
        val viewModel = MetricasViewModel(MetricasFalsas(), reportes) { hoy }
        val carpeta = carpetas.newFolder("documentos")

        viewModel.descargarPdf(carpeta)
        advanceUntilIdle()
        reportes.hayConexion = true
        viewModel.descargarPdf(carpeta)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertNull(estado.errorDescarga)
        assertNotNull(estado.reporte)
        assertEquals(1, archivosEn(carpeta).size)
    }

    @Test
    fun siLaRespuestaNoEsPdfNoQuedaNingunArchivo() = runTest(despachador) {
        // Por ejemplo, el JSON de error de Spring en lugar del PDF.
        val json = """{"codigo":"ERROR","mensaje":"falló"}""".toByteArray()
        val viewModel = MetricasViewModel(MetricasFalsas(), ReportesFalsos(hayConexion = true, bytes = json)) { hoy }
        val carpeta = carpetas.newFolder("documentos")

        viewModel.descargarPdf(carpeta)
        advanceUntilIdle()

        assertNull(viewModel.estado.value.reporte)
        assertEquals("No se pudo guardar el PDF en el celular.", viewModel.estado.value.errorDescarga)
        assertTrue(archivosEn(carpeta).isEmpty())
    }

    @Test
    fun unaRespuestaVaciaNoSeGuarda() {
        val carpeta = carpetas.newFolder("documentos")

        assertFalse(GuardadoPdf.esPdf(ByteArray(0)))
        assertFalse(GuardadoPdf.esPdf("%PDF".toByteArray()))
        try {
            GuardadoPdf.guardar(ByteArray(0), carpeta, "vacio.pdf")
            throw AssertionError("Debió rechazar la respuesta vacía.")
        } catch (esperado: IOException) {
            // Correcto: no es un PDF.
        }
        assertTrue(archivosEn(carpeta).isEmpty())
    }

    @Test
    fun guardarCreaLaCarpetaYReemplazaElPdfAnterior() {
        val carpeta = File(carpetas.root, "nueva/documentos")

        GuardadoPdf.guardar("%PDF-1.4 viejo".toByteArray(), carpeta, "informe.pdf")
        val archivo = GuardadoPdf.guardar(pdfDePrueba, carpeta, "informe.pdf")

        assertTrue(archivo.readBytes().contentEquals(pdfDePrueba))
        // Solo el PDF final: no queda el temporal ".parcial".
        assertEquals(listOf("informe.pdf"), archivosEn(carpeta))
    }
}
