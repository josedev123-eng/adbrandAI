package com.adbrand.ui.revision

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.EstadosContenidoRepository
import com.adbrand.domain.model.ContenidoConEstado
import com.adbrand.ui.revision.viewmodel.EstadosContenidoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// HU 63 en el móvil: "Todos" sin los cancelados, de a 12 con "Ver más", buscador, cancelar, activar y eliminar.
@OptIn(ExperimentalCoroutinesApi::class)
class MisAnunciosMovilTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(despachador)

    @After
    fun terminar() = Dispatchers.resetMain()

    private class Falso(var lista: List<ContenidoConEstado>) : EstadosContenidoRepository {
        override suspend fun estados(): Resultado<List<ContenidoConEstado>> = Resultado.Exito(lista)
        override suspend fun cancelar(id: Long): Resultado<Int> = Resultado.Exito(1)
        override suspend fun reactivar(id: Long): Resultado<Unit> = Resultado.Exito(Unit)
        override suspend fun eliminar(id: Long): Resultado<Unit> = Resultado.Exito(Unit)
    }

    private fun anuncios() = (1..14L).map {
        ContenidoConEstado(
            id = it,
            titular = if (it == 1L) "Panetón navideño" else "Promo $it",
            estadoActual = if (it == 14L) "RECHAZADO" else "APROBADO",
            fecha = if (it == 2L) "2026-12-01" else "2026-11-20"
        )
    }

    @Test
    fun todosNoMuestraLosCanceladosYSeVenDeA12() = runTest {
        val vm = EstadosContenidoViewModel(Falso(anuncios()))
        vm.cargar()
        advanceUntilIdle()

        assertEquals(12, vm.estado.value.visibles.size)
        assertEquals(1, vm.estado.value.restantes)
        assertEquals(13, vm.estado.value.cantidadTodos)
        assertEquals(1, vm.estado.value.cantidadCancelados)

        vm.verMas()
        assertEquals(13, vm.estado.value.visibles.size)
        vm.verCancelados(true)
        assertEquals(listOf(14L), vm.estado.value.visibles.map { it.id })
    }

    @Test
    fun buscaPorTextoSinTildesOPorFecha() = runTest {
        val vm = EstadosContenidoViewModel(Falso(anuncios()))
        vm.cargar()
        advanceUntilIdle()

        vm.buscar("paneton")
        assertEquals(listOf(1L), vm.estado.value.visibles.map { it.id })
        vm.buscar("2026-12-01")
        assertEquals(listOf(2L), vm.estado.value.visibles.map { it.id })
    }

    @Test
    fun cancelarLoPasaACanceladosYEliminarLoQuita() = runTest {
        val vm = EstadosContenidoViewModel(Falso(anuncios()))
        vm.cargar()
        advanceUntilIdle()

        vm.cancelar(3)
        advanceUntilIdle()
        assertTrue(vm.estado.value.contenidos.first { it.id == 3L }.cancelado)
        assertTrue(vm.estado.value.mensaje!!.contains("Bórralo si ya lo subiste"))

        vm.eliminar(3)
        advanceUntilIdle()
        assertTrue(vm.estado.value.contenidos.none { it.id == 3L })
    }
}
