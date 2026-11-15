package com.adbrand.ui.anuncio

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AnuncioRepository
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.RedSocial
import com.adbrand.domain.model.Tono
import com.adbrand.ui.anuncio.viewmodel.CrearAnuncioViewModel
import com.adbrand.ui.anuncio.viewmodel.MAXIMO_OFERTA
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

// HU 10, 12 y 13: crear, regenerar y validar anuncios sin tocar la red.
@OptIn(ExperimentalCoroutinesApi::class)
class CrearAnuncioViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun terminar() {
        Dispatchers.resetMain()
    }

    private class AnuncioFalso(
        private val generado: Resultado<AnuncioGenerado> = Resultado.Exito(anuncio()),
        private val regenerado: Resultado<AnuncioGenerado> = Resultado.Exito(
            anuncio(texto = "Texto regenerado", id = 7L)
        )
    ) : AnuncioRepository {
        var ofertaEnviada: String? = null
        var redEnviada: RedSocial? = null
        var idRegenerado: Long? = null

        override suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<AnuncioGenerado> {
            ofertaEnviada = oferta
            redEnviada = redSocial
            return generado
        }

        override suspend fun regenerar(id: Long): Resultado<AnuncioGenerado> {
            idRegenerado = id
            return regenerado
        }
    }

    @Test
    fun unaOfertaVaciaSeValidaSinLlamarAlBackend() = runTest {
        val repositorio = AnuncioFalso()
        val viewModel = CrearAnuncioViewModel(repositorio)

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("Escribe la oferta que quieres anunciar.", viewModel.estado.value.errorOferta)
        assertNull(repositorio.ofertaEnviada)
        assertFalse(viewModel.estado.value.generando)
    }

    @Test
    fun unaOfertaDemasiadoLargaSeRechaza() = runTest {
        val repositorio = AnuncioFalso()
        val viewModel = CrearAnuncioViewModel(repositorio)
        viewModel.actualizarOferta("a".repeat(MAXIMO_OFERTA + 1))

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("Máximo $MAXIMO_OFERTA caracteres.", viewModel.estado.value.errorOferta)
        assertNull(repositorio.ofertaEnviada)
    }

    @Test
    fun generarGuardaElAnuncioYLaRedElegida() = runTest {
        val repositorio = AnuncioFalso()
        val viewModel = CrearAnuncioViewModel(repositorio)
        viewModel.actualizarOferta("2 panetones por S/ 35")
        viewModel.actualizarRedSocial(RedSocial.FACEBOOK)

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("2 panetones por S/ 35", repositorio.ofertaEnviada)
        assertEquals(RedSocial.FACEBOOK, repositorio.redEnviada)
        assertEquals("Texto de prueba", viewModel.estado.value.anuncio?.texto)
        assertFalse(viewModel.estado.value.generando)
        assertFalse(viewModel.estado.value.faltaPerfil)
    }

    @Test
    fun sinPerfilSeAvisaParaCompletarlo() = runTest {
        val viewModel = CrearAnuncioViewModel(
            AnuncioFalso(
                generado = Resultado.Error(
                    mensaje = "Completa el perfil de tu negocio.",
                    codigo = 404,
                    codigoError = "PERFIL_NO_ENCONTRADO"
                )
            )
        )
        viewModel.actualizarOferta("Oferta válida")

        viewModel.generar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.faltaPerfil)
        assertNull(viewModel.estado.value.anuncio)
    }

    @Test
    fun unErrorDeValidacionSeMuestraEnElCampoOferta() = runTest {
        val viewModel = CrearAnuncioViewModel(
            AnuncioFalso(
                generado = Resultado.Error(
                    mensaje = "Revisa la oferta.",
                    codigo = 400,
                    campos = mapOf("oferta" to "La oferta no puede tener enlaces.")
                )
            )
        )
        viewModel.actualizarOferta("Oferta con enlace")

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("La oferta no puede tener enlaces.", viewModel.estado.value.errorOferta)
        assertNull(viewModel.estado.value.aviso)
    }

    @Test
    fun regenerarUsaElIdDelAnuncioActual() = runTest {
        val repositorio = AnuncioFalso()
        val viewModel = CrearAnuncioViewModel(repositorio)
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()

        viewModel.regenerar()
        advanceUntilIdle()

        assertEquals(7L, repositorio.idRegenerado)
        assertEquals("Texto regenerado", viewModel.estado.value.anuncio?.texto)
        assertFalse(viewModel.estado.value.regenerando)
    }

    @Test
    fun copiarMarcaElAnuncioComoCopiado() = runTest {
        val viewModel = CrearAnuncioViewModel(AnuncioFalso())
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()

        viewModel.marcarCopiado()

        assertTrue(viewModel.estado.value.copiado)
    }

    private companion object {
        fun anuncio(
            texto: String = "Texto de prueba",
            id: Long = 7L
        ) = AnuncioGenerado(
            id = id,
            texto = texto,
            redSocial = RedSocial.INSTAGRAM,
            tono = Tono.CERCANO,
            simulado = true,
            estado = "APROBADO"
        )
    }
}
