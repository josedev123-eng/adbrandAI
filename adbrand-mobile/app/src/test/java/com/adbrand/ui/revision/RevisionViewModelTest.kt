package com.adbrand.ui.revision

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.RevisionRepository
import com.adbrand.domain.model.ContenidoRevision
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 53: aprobar contenido desde el móvil, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class RevisionViewModelTest {

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
        private val lista: Resultado<List<ContenidoRevision>>,
        private val alAprobar: Resultado<Unit> = Resultado.Exito(Unit)
    ) : RevisionRepository {
        val aprobados = mutableListOf<Long>()

        override suspend fun porRevisar(): Resultado<List<ContenidoRevision>> = lista

        override suspend fun aprobar(id: Long): Resultado<Unit> {
            aprobados += id
            return alAprobar
        }
    }

    private val pendiente = ContenidoRevision(id = 1L, oferta = "2x1 en café", texto = "Ven con un amigo", puedeAprobar = true)
    private val rechazado = ContenidoRevision(
        id = 2L, oferta = "Cigarrillos", texto = "Oferta", estado = "RECHAZADO",
        motivoRevision = "Publicidad de tabaco.", puedeAprobar = false
    )

    @Test
    fun cargaLaListaPorRevisar() = runTest {
        val viewModel = RevisionViewModel(RepositorioFalso(Resultado.Exito(listOf(pendiente, rechazado))))

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals(2, viewModel.estado.value.contenidos.size)
        assertTrue(viewModel.estado.value.cargado)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun alAprobarSaleDeLaListaYMuestraElAviso() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(listOf(pendiente, rechazado)))
        val viewModel = RevisionViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.aprobar(1L)
        advanceUntilIdle()

        assertEquals(listOf(1L), repositorio.aprobados)
        assertEquals(listOf(2L), viewModel.estado.value.contenidos.map { it.id })
        assertEquals("Contenido aprobado. Ya puedes programarlo en el calendario.", viewModel.estado.value.aviso)
        assertNull(viewModel.estado.value.procesandoId)
    }

    @Test
    fun loRechazadoPorElModeradorNoSeEnviaAAprobar() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(listOf(pendiente, rechazado)))
        val viewModel = RevisionViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.aprobar(2L)
        advanceUntilIdle()

        assertTrue(repositorio.aprobados.isEmpty())
        assertEquals(2, viewModel.estado.value.contenidos.size)
    }

    @Test
    fun siElServidorNoApruebaElContenidoSigueEnLaListaConElMensaje() = runTest {
        val viewModel = RevisionViewModel(
            RepositorioFalso(
                Resultado.Exito(listOf(pendiente)),
                Resultado.Error("Este contenido todavía lo está revisando un moderador.", 400)
            )
        )
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.aprobar(1L)
        advanceUntilIdle()

        assertEquals(1, viewModel.estado.value.contenidos.size)
        assertEquals("Este contenido todavía lo está revisando un moderador.", viewModel.estado.value.error)
        assertNull(viewModel.estado.value.aviso)
    }

    @Test
    fun unErrorAlCargarSeMuestraEnLaPantalla() = runTest {
        val viewModel = RevisionViewModel(RepositorioFalso(Resultado.Error("No se pudo cargar el contenido por revisar.", 500)))

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("No se pudo cargar el contenido por revisar.", viewModel.estado.value.error)
        assertTrue(viewModel.estado.value.cargado)
    }
}
