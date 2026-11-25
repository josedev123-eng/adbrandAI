package com.adbrand.ui.revision

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.EstadosContenidoRepository
import com.adbrand.data.repository.RevisionRepository
import com.adbrand.domain.model.ContenidoConEstado
import com.adbrand.domain.model.ContenidoRevision
import com.adbrand.ui.revision.viewmodel.EstadosContenidoViewModel
import com.adbrand.ui.revision.viewmodel.RevisionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 56: después de aprobar, rechazar o deslizar hacia abajo, la lista muestra el estado nuevo. Sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class EstadosContenidoViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    // Hace de Spring: guarda los contenidos y su estado. Aprobar o rechazar lo cambia y lo pone primero,
    // igual que GET /api/contenido/estados (lo último que cambió sale arriba).
    private class ServidorFalso {
        val contenidos = mutableListOf(
            ContenidoConEstado(1, "2x1 en café", estadoActual = "PENDIENTE"),
            ContenidoConEstado(2, "Postre gratis", estadoActual = "PENDIENTE")
        )
        var hayConexion = true

        fun cambiar(id: Long, estadoActual: String, motivo: String = "") {
            val actual = contenidos.first { it.id == id }
            contenidos.remove(actual)
            contenidos.add(0, actual.copy(estadoActual = estadoActual, motivo = motivo))
        }
    }

    private class EstadosFalsos(private val servidor: ServidorFalso) : EstadosContenidoRepository {
        var pedidos = 0
        override suspend fun estados(): Resultado<List<ContenidoConEstado>> {
            pedidos++
            if (!servidor.hayConexion) return Resultado.Error("No se pudo conectar con el servidor. Revisa tu conexión.")
            return Resultado.Exito(servidor.contenidos.toList())
        }
    }

    // Los botones de Jose (HU 53 y 54) contra el mismo servidor falso.
    private class RevisionFalsa(private val servidor: ServidorFalso) : RevisionRepository {
        override suspend fun porRevisar(): Resultado<List<ContenidoRevision>> = Resultado.Exito(
            servidor.contenidos.filter { it.estadoActual == "PENDIENTE" }.map { ContenidoRevision(it.id, it.oferta) }
        )

        override suspend fun aprobar(id: Long): Resultado<Unit> {
            servidor.cambiar(id, "APROBADO")
            return Resultado.Exito(Unit)
        }

        override suspend fun rechazar(id: Long, motivo: String?): Resultado<Unit> {
            servidor.cambiar(id, "RECHAZADO", motivo ?: "")
            return Resultado.Exito(Unit)
        }
    }

    @Test
    fun alAprobarElContenidoApareceArribaComoAprobado() = runTest(despachador) {
        val servidor = ServidorFalso()
        val revision = RevisionViewModel(RevisionFalsa(servidor))
        val estados = EstadosContenidoViewModel(EstadosFalsos(servidor))
        revision.cargar()
        estados.cargar()
        advanceUntilIdle()

        revision.aprobar(2)
        advanceUntilIdle()
        // La pantalla recarga la sección cuando aparece el aviso de Jose.
        assertNotNull(revision.estado.value.aviso)
        estados.cargar()
        advanceUntilIdle()

        val primero = estados.estado.value.contenidos.first()
        assertEquals(2L, primero.id)
        assertEquals("APROBADO", primero.estadoActual)
        assertEquals("Aprobado", primero.etiqueta)
        assertEquals("PENDIENTE", estados.estado.value.contenidos[1].estadoActual)
    }

    @Test
    fun alRechazarSeVeElEstadoYElMotivo() = runTest(despachador) {
        val servidor = ServidorFalso()
        val revision = RevisionViewModel(RevisionFalsa(servidor))
        val estados = EstadosContenidoViewModel(EstadosFalsos(servidor))
        revision.cargar()
        advanceUntilIdle()

        revision.pedirRechazo(1)
        revision.cambiarMotivo("El precio está mal")
        revision.confirmarRechazo()
        advanceUntilIdle()
        estados.cargar()
        advanceUntilIdle()

        val primero = estados.estado.value.contenidos.first()
        assertEquals(1L, primero.id)
        assertEquals("Rechazado", primero.etiqueta)
        assertEquals("El precio está mal", primero.motivo)
    }

    @Test
    fun alDeslizarHaciaAbajoSeVeLoQueDecidioElModerador() = runTest(despachador) {
        val servidor = ServidorFalso()
        val repositorio = EstadosFalsos(servidor)
        val estados = EstadosContenidoViewModel(repositorio)
        estados.cargar()
        advanceUntilIdle()

        // Mientras tanto, un moderador rechaza el contenido desde el panel.
        servidor.cambiar(1, "RECHAZADO_MODERADOR", "Usa palabras no permitidas.")
        estados.cargar()
        advanceUntilIdle()

        assertEquals(2, repositorio.pedidos)
        val primero = estados.estado.value.contenidos.first()
        assertEquals("Rechazado por el moderador", primero.etiqueta)
        assertEquals("Usa palabras no permitidas.", primero.motivo)
    }

    @Test
    fun sinConexionSeAvisaYSeQuedaLaUltimaLista() = runTest(despachador) {
        val servidor = ServidorFalso()
        val estados = EstadosContenidoViewModel(EstadosFalsos(servidor))
        estados.cargar()
        advanceUntilIdle()

        servidor.hayConexion = false
        estados.cargar()
        advanceUntilIdle()

        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión.", estados.estado.value.error)
        assertEquals(2, estados.estado.value.contenidos.size)

        // Al volver la conexión, "Reintentar" quita el aviso.
        servidor.hayConexion = true
        estados.cargar()
        advanceUntilIdle()
        assertNull(estados.estado.value.error)
    }

    @Test
    fun sinContenidosLaListaQuedaVacia() = runTest(despachador) {
        val servidor = ServidorFalso().apply { contenidos.clear() }
        val estados = EstadosContenidoViewModel(EstadosFalsos(servidor))

        estados.cargar()
        advanceUntilIdle()

        assertTrue(estados.estado.value.cargado)
        assertTrue(estados.estado.value.contenidos.isEmpty())
        assertNull(estados.estado.value.error)
    }

    @Test
    fun cadaEstadoTieneSuEtiqueta() {
        assertEquals("Pendiente", ContenidoConEstado(1, estadoActual = "PENDIENTE").etiqueta)
        assertEquals("Aprobado", ContenidoConEstado(1, estadoActual = "APROBADO").etiqueta)
        assertEquals("Rechazado", ContenidoConEstado(1, estadoActual = "RECHAZADO").etiqueta)
        assertEquals("En revisión", ContenidoConEstado(1, estadoActual = "EN_REVISION").etiqueta)
        assertEquals("Rechazado por el moderador", ContenidoConEstado(1, estadoActual = "RECHAZADO_MODERADOR").etiqueta)
    }
}
