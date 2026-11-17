package com.adbrand.ui.previsualizacion

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.PrevisualizacionRepository
import com.adbrand.domain.model.Anuncio
import com.adbrand.domain.model.FormatoVista
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

// HU 45 y 46: previsualización con kit y sin kit, y el anuncio como publicación, sin red.
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
        private val resultado: Resultado<Previsualizacion>,
        private val alPedirImagen: Resultado<String?> = Resultado.Exito("data:image/jpeg;base64,QUJD")
    ) : PrevisualizacionRepository {
        val imagenesPedidas = mutableListOf<Long>()
        val formatosPedidos = mutableListOf<String>()
        override suspend fun obtener(formato: String): Resultado<Previsualizacion> {
            formatosPedidos += formato
            return if (resultado is Resultado.Exito) Resultado.Exito(resultado.dato.copy(formato = formatoDe(formato))) else resultado
        }
        override suspend fun imagen(contenidoId: Long): Resultado<String?> = alPedirImagen.also { imagenesPedidas += contenidoId }
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

    @Test
    fun alTocarUnAnuncioSeAbreComoPublicacionConSuImagen() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        val viewModel = PrevisualizacionViewModel(repositorio)

        viewModel.abrirPublicacion(anuncio())
        assertEquals(true, viewModel.estado.value.cargandoImagen)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(10L, estado.publicacion?.id)
        assertEquals("data:image/jpeg;base64,QUJD", estado.imagen)
        assertEquals(false, estado.cargandoImagen)
        assertEquals(listOf(10L), repositorio.imagenesPedidas)
    }

    @Test
    fun alElegirOtroFormatoSeVuelvenAPedirLosAnunciosEnEseFormato() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        val viewModel = PrevisualizacionViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()
        assertEquals("CUADRADO", viewModel.estado.value.formato.codigo)

        viewModel.cambiarFormato("VERTICAL")
        advanceUntilIdle()
        assertEquals("VERTICAL", viewModel.estado.value.formato.codigo)
        viewModel.cambiarFormato("VERTICAL")
        advanceUntilIdle()

        viewModel.cambiarFormato("HORIZONTAL")
        advanceUntilIdle()
        assertEquals("HORIZONTAL", viewModel.estado.value.formato.codigo)
        // Elegir el mismo formato otra vez no vuelve a pedir nada.
        assertEquals(listOf("CUADRADO", "VERTICAL", "HORIZONTAL"), repositorio.formatosPedidos)
    }

    @Test
    fun cadaFormatoTieneSuProporcion() {
        assertEquals(1f, com.adbrand.ui.previsualizacion.screens.proporcion(formatoDe("CUADRADO")))
        assertEquals(0.5625f, com.adbrand.ui.previsualizacion.screens.proporcion(formatoDe("VERTICAL")))
        assertEquals(1200f / 628f, com.adbrand.ui.previsualizacion.screens.proporcion(formatoDe("HORIZONTAL")))
    }

    @Test
    fun unAnuncioAcortadoSeMarcaParaMostrarElAviso() = runTest {
        val largo = anuncio().copy(textoAjustado = "Hoy hay ofertas…", recortado = true)
        val viewModel = PrevisualizacionViewModel(RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(largo)))))
        viewModel.cargar()
        advanceUntilIdle()

        assertEquals(true, viewModel.estado.value.anuncios.first().recortado)
        assertEquals("Hoy hay ofertas…", viewModel.estado.value.anuncios.first().textoAjustado)
    }

    @Test
    fun alCerrarLaVistaPreviaSeLimpia() = runTest {
        val viewModel = PrevisualizacionViewModel(RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio())))))
        viewModel.abrirPublicacion(anuncio())
        advanceUntilIdle()

        viewModel.cerrarPublicacion()
        assertNull(viewModel.estado.value.publicacion)
        assertNull(viewModel.estado.value.imagen)
    }

    @Test
    fun siLaImagenFallaSeMuestraIgualConUnAviso() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(null, listOf(anuncio()))), Resultado.Error("Sin conexión"))
        val viewModel = PrevisualizacionViewModel(repositorio)

        viewModel.abrirPublicacion(anuncio())
        advanceUntilIdle()

        assertNotNull(viewModel.estado.value.publicacion)
        assertEquals("Sin conexión", viewModel.estado.value.errorImagen)
    }

}

private fun formatoDe(codigo: String) = when (codigo) {
    "VERTICAL" -> FormatoVista("VERTICAL", "Vertical", "Historia", 1080, 1920, 90)
    "HORIZONTAL" -> FormatoVista("HORIZONTAL", "Horizontal", "Horizontal", 1200, 628, 100)
    else -> FormatoVista()
}
