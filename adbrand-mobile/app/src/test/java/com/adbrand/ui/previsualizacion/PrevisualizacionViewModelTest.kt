package com.adbrand.ui.previsualizacion

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PrevisualizacionRepository
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.KitMarca
import com.adbrand.domain.model.LogoMarca
import com.adbrand.domain.model.Previsualizacion
import com.adbrand.ui.previsualizacion.viewmodel.PrevisualizacionViewModel
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
import org.junit.Before
import org.junit.Test

// HU 45: previsualización con kit y sin kit, sin red.
@OptIn(ExperimentalCoroutinesApi::class)
class PrevisualizacionViewModelTest {

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
        private val resultado: Resultado<Previsualizacion>
    ) : PrevisualizacionRepository {
        override suspend fun obtener(): Resultado<Previsualizacion> = resultado
    }

    private fun kit() = KitMarca(
        id = 1L,
        nombreNegocio = "Bodega San Martín",
        sector = "Comercio",
        logoConcepto = "Una bolsa de compras",
        tipografiaPrimaria = "Poppins",
        tipografiaSecundaria = "Inter",
        paletaColores = "#4F46E5,#F97316",
        vozMarca = "Cercana",
        logoDiseno = LogoMarca(texto = "BS", forma = "CIRCULO", colorFondo = "#4F46E5", colorTexto = "#FFFFFF")
    )

    private fun anuncio() = Anuncio(
        id = 10L,
        texto = "Hoy hay ofertas en la bodega",
        redSocial = "INSTAGRAM",
        tono = "CERCANO",
        estado = "APROBADO"
    )

    @Test
    fun conKitSeCarganElKitYLosAnuncios() = runTest {
        val viewModel = PrevisualizacionViewModel(
            RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        )

        viewModel.cargar()
        advanceUntilIdle()

        assertNotNull(viewModel.estado.value.kit)
        assertEquals("Bodega San Martín", viewModel.estado.value.kit?.nombreNegocio)
        assertEquals(1, viewModel.estado.value.anuncios.size)
        assertEquals("Hoy hay ofertas en la bodega", viewModel.estado.value.anuncios.first().texto)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun sinKitLaPantallaMuestraElAvisoYLosAnunciosSiguen() = runTest {
        val viewModel = PrevisualizacionViewModel(
            RepositorioFalso(Resultado.Exito(Previsualizacion(null, listOf(anuncio()))))
        )

        viewModel.cargar()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.kit)
        assertEquals(1, viewModel.estado.value.anuncios.size)
        assertEquals(true, viewModel.estado.value.cargado)
    }

    @Test
    fun unErrorSeMuestraEnLaPantalla() = runTest {
        val viewModel = PrevisualizacionViewModel(
            RepositorioFalso(Resultado.Error("No se pudo cargar la previsualización.", 500))
        )

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals("No se pudo cargar la previsualización.", viewModel.estado.value.error)
        assertEquals(true, viewModel.estado.value.cargado)
    }
}
