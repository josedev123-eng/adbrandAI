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
        val compartidos = mutableListOf<Pair<Long, String>>()
        override suspend fun registrarCompartido(contenidoId: Long, formato: String): Resultado<Unit> {
            compartidos += contenidoId to formato
            return Resultado.Exito(Unit)
        }
        // Ajuste 14: el anuncio diseñado por formato.
        val disenosPedidos = mutableListOf<String>()
        override suspend fun imagenFinal(contenidoId: Long, formato: String): Resultado<ByteArray> {
            disenosPedidos += formato
            return Resultado.Exito(formato.toByteArray())
        }
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


    // HU 48: zoom, recorte y restablecer en el estado de la pantalla.
    @Test
    fun elZoomSeGuardaYRestablecerLoVuelveAlOriginal() = runTest {
        val viewModel = PrevisualizacionViewModel(RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio())))))
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.abrirPublicacion(anuncio())
        advanceUntilIdle()

        viewModel.ajustar(zoom = 2f, arrastreX = 40f, arrastreY = 0f, anchoMarco = 500f, anchoImagen = 1000, altoImagen = 1000)
        assertEquals(2f, viewModel.estado.value.ajuste.escala)
        assertEquals(false, viewModel.estado.value.ajuste.esOriginal)

        viewModel.restablecer()
        assertEquals(true, viewModel.estado.value.ajuste.esOriginal)
    }

    @Test
    fun alCambiarDeFormatoElRecorteVuelveAlOriginal() = runTest {
        val viewModel = PrevisualizacionViewModel(RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio())))))
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.ajustar(zoom = 3f, arrastreX = 0f, arrastreY = 0f, anchoMarco = 500f, anchoImagen = 1000, altoImagen = 1000)

        viewModel.cambiarFormato("VERTICAL")
        advanceUntilIdle()

        assertEquals(true, viewModel.estado.value.ajuste.esOriginal)
    }

    // HU 47: compartir la previsualización.
    private suspend fun kotlinx.coroutines.test.TestScope.abierto(repositorio: RepositorioFalso): PrevisualizacionViewModel {
        val viewModel = PrevisualizacionViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.cambiarFormato("VERTICAL")
        advanceUntilIdle()
        viewModel.abrirPublicacion(anuncio())
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun alCompartirSeAbreElMenuYQuedaRegistradoConElFormato() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        val viewModel = abierto(repositorio)
        val menusAbiertos = mutableListOf<String>()

        viewModel.compartir({ "imagen-generada.jpg" }, { menusAbiertos += it })
        advanceUntilIdle()

        assertEquals(listOf("imagen-generada.jpg"), menusAbiertos)
        assertEquals(listOf(10L to "VERTICAL"), repositorio.compartidos)
        assertNull(viewModel.estado.value.mensajeCompartir)
    }

    @Test
    fun siNoSePudoGenerarLaImagenAvisaYNoAbreElMenu() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        val viewModel = abierto(repositorio)
        val menusAbiertos = mutableListOf<String>()

        viewModel.compartir<String>({ null }, { menusAbiertos += it })
        viewModel.compartir<String>({ throw IllegalStateException("sin memoria") }, { menusAbiertos += it })
        advanceUntilIdle()

        assertEquals(emptyList<String>(), menusAbiertos)
        assertEquals(emptyList<Pair<Long, String>>(), repositorio.compartidos)
        assertEquals(PrevisualizacionViewModel.MENSAJE_NO_SE_PUDO_COMPARTIR, viewModel.estado.value.mensajeCompartir)
    }

    @Test
    fun guardarEnLaGaleriaAvisaSiFuncionoONo() = runTest {
        val viewModel = abierto(RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio())))))

        viewModel.guardar { true }
        assertEquals(true, viewModel.estado.value.mensajeCompartir!!.contains("galería"))
        viewModel.guardar { false }
        assertEquals(true, viewModel.estado.value.mensajeCompartir!!.startsWith("No se pudo guardar"))
    }

    @Test
    fun elMensajeParaWhatsAppYCorreoLlevaElTextoYElNegocio() {
        val mensaje = com.adbrand.ui.previsualizacion.recorte.MensajeCompartir.para("Panadería Doña Rosa", " 2x1 en pan de yema ")

        assertEquals("Anuncio de Panadería Doña Rosa", mensaje.asunto)
        assertEquals("2x1 en pan de yema\n\n— Panadería Doña Rosa", mensaje.cuerpo)
        assertEquals("Anuncio de mi negocio", com.adbrand.ui.previsualizacion.recorte.MensajeCompartir.para("", "Hola").asunto)
    }

    // HU 61: lo que se comparte lleva el eslogan del kit en la firma.
    @Test
    fun elMensajeCompartidoLlevaElEsloganDelKit() {
        val mensaje = com.adbrand.ui.previsualizacion.recorte.MensajeCompartir.para(
            "Panadería Rosa", "2x1 en pan", "“El sabor de tu barrio”"
        )

        assertEquals("2x1 en pan\n\n— Panadería Rosa: “El sabor de tu barrio”", mensaje.cuerpo)
    }

    // Ajuste 14: al abrir un anuncio llega su diseño (plantilla de la opción elegida) y se pide de nuevo al cambiar de formato.
    @Test
    fun alAbrirUnAnuncioLlegaSuDisenoYCambiaConElFormato() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        val viewModel = PrevisualizacionViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.abrirPublicacion(anuncio())
        advanceUntilIdle()
        assertEquals("CUADRADO", viewModel.estado.value.diseno?.decodeToString())

        viewModel.cambiarFormato("VERTICAL")
        advanceUntilIdle()
        assertEquals("VERTICAL", viewModel.estado.value.diseno?.decodeToString())
        assertEquals(listOf("CUADRADO", "VERTICAL"), repositorio.disenosPedidos)
    }

    // "Subir a mi estado": siempre pide la imagen vertical y se la pasa a la pantalla para abrir WhatsApp.
    @Test
    fun subirAMiEstadoPideLaImagenVertical() = runTest {
        val repositorio = RepositorioFalso(Resultado.Exito(Previsualizacion(kit(), listOf(anuncio()))))
        val viewModel = PrevisualizacionViewModel(repositorio)
        viewModel.abrirPublicacion(anuncio())
        advanceUntilIdle()

        var recibido: String? = null
        viewModel.subirAEstado { recibido = it.decodeToString() }
        advanceUntilIdle()

        assertEquals("VERTICAL", recibido)
        assertEquals(false, viewModel.estado.value.subiendoEstado)
    }
}

private fun formatoDe(codigo: String) = when (codigo) {
    "VERTICAL" -> FormatoVista("VERTICAL", "Vertical", "Historia", 1080, 1920, 90)
    "HORIZONTAL" -> FormatoVista("HORIZONTAL", "Horizontal", "Horizontal", 1200, 628, 100)
    else -> FormatoVista()
}
