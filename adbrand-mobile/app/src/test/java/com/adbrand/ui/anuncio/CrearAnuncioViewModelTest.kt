package com.adbrand.ui.anuncio

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AnuncioOpcionesRepository
import com.adbrand.data.repository.AnuncioRepository
import com.adbrand.domain.model.AnuncioGenerado
import com.adbrand.domain.model.OpcionAnuncio
import com.adbrand.domain.model.OpcionElegida
import com.adbrand.domain.model.OpcionesAnuncio
import com.adbrand.domain.model.RedSocial
import com.adbrand.domain.model.Tono
import com.adbrand.domain.model.VersionAnuncio
import com.adbrand.domain.model.VersionesAnuncio
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

// HU 10, 12 y 13 + IA de imágenes: generar 4 opciones, elegir una y regenerar
// el texto sin tocar la red.
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
        private val regenerado: Resultado<AnuncioGenerado> = Resultado.Exito(
            anuncio(texto = "Texto regenerado", id = 7L)
        )
    ) : AnuncioRepository {
        var idRegenerado: Long? = null

        override suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<AnuncioGenerado> =
            Resultado.Exito(anuncio())

        override suspend fun regenerar(id: Long): Resultado<AnuncioGenerado> {
            idRegenerado = id
            return regenerado
        }
    }

    private class OpcionesFalso(
        private val generadas: Resultado<OpcionesAnuncio> = Resultado.Exito(opciones()),
        private val elegida: Resultado<OpcionElegida> = Resultado.Exito(
            OpcionElegida(anuncio = anuncio(), opcion = opcion(numero = 2, elegida = true))
        ),
        private val retocada: Resultado<VersionesAnuncio> = Resultado.Exito(versiones(actual = 2)),
        private val historial: Resultado<VersionesAnuncio> = Resultado.Exito(versiones(actual = 1))
    ) : AnuncioOpcionesRepository {
        var ofertaEnviada: String? = null
        var redEnviada: RedSocial? = null
        var idElegido: Long? = null
        var instruccionEnviada: String? = null
        var versionUsada: Int? = null

        override suspend fun generar(oferta: String, redSocial: RedSocial): Resultado<OpcionesAnuncio> {
            ofertaEnviada = oferta
            redEnviada = redSocial
            return generadas
        }

        var fotoEnviada: String? = null
        override suspend fun generar(oferta: String, redSocial: RedSocial, foto: String?): Resultado<OpcionesAnuncio> {
            fotoEnviada = foto
            return generar(oferta, redSocial)
        }

        override suspend fun elegir(opcionId: Long): Resultado<OpcionElegida> {
            idElegido = opcionId
            return elegida
        }

        override suspend fun retocar(opcionId: Long, instruccion: String): Resultado<VersionesAnuncio> {
            instruccionEnviada = instruccion
            return retocada
        }

        override suspend fun versiones(opcionId: Long): Resultado<VersionesAnuncio> = historial

        override suspend fun usarVersion(opcionId: Long, numero: Int): Resultado<VersionesAnuncio> {
            versionUsada = numero
            return Resultado.Exito(versiones(actual = numero))
        }
    }

    @Test
    fun unaOfertaVaciaSeValidaSinLlamarAlBackend() = runTest {
        val opciones = OpcionesFalso()
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("Escribe la oferta que quieres anunciar.", viewModel.estado.value.errorOferta)
        assertNull(opciones.ofertaEnviada)
        assertFalse(viewModel.estado.value.generando)
    }

    // HU 67: la foto del producto se manda con el pedido y, si no se pudo hacer la escena, se muestra el aviso.
    @Test
    fun laFotoDelProductoSeMandaConElPedidoYSeMuestraElAvisoDeLaEscena() = runTest {
        val opciones = OpcionesFalso(Resultado.Exito(opciones().copy(avisoFoto = "Usamos tu foto tal cual.")))
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("Mi torta en una fiesta")
        viewModel.ponerFoto("image/png", byteArrayOf(1, 2, 3))

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("data:image/png;base64,AQID", opciones.fotoEnviada)
        assertEquals("Usamos tu foto tal cual.", viewModel.estado.value.avisoFoto)
    }

    @Test
    fun unaFotoQueNoEsJpgNiPngOMuyPesadaSeRechaza() = runTest {
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), OpcionesFalso())

        viewModel.ponerFoto("image/gif", byteArrayOf(1))
        assertEquals("Sube tu foto en JPG o PNG.", viewModel.estado.value.errorFoto)
        viewModel.ponerFoto("image/jpeg", ByteArray(5 * 1024 * 1024 + 1))
        assertEquals("La foto puede pesar hasta 5 MB.", viewModel.estado.value.errorFoto)
        assertNull(viewModel.estado.value.foto)

        viewModel.ponerFoto("image/jpeg", byteArrayOf(1))
        viewModel.quitarFoto()
        assertNull(viewModel.estado.value.foto)
    }

    @Test
    fun unaOfertaDemasiadoLargaSeRechaza() = runTest {
        val opciones = OpcionesFalso()
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("a".repeat(MAXIMO_OFERTA + 1))

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("Máximo $MAXIMO_OFERTA caracteres.", viewModel.estado.value.errorOferta)
        assertNull(opciones.ofertaEnviada)
    }

    @Test
    fun generarGuardaLasCuatroOpcionesYLaRedElegida() = runTest {
        val opciones = OpcionesFalso()
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("2 panetones por S/ 35")
        viewModel.actualizarRedSocial(RedSocial.FACEBOOK)

        viewModel.generar()
        advanceUntilIdle()

        assertEquals("2 panetones por S/ 35", opciones.ofertaEnviada)
        assertEquals(RedSocial.FACEBOOK, opciones.redEnviada)
        assertEquals(4, viewModel.estado.value.opciones.size)
        assertEquals("Propuesta 1", viewModel.estado.value.opciones.first().texto)
        assertTrue(viewModel.estado.value.hayOpciones)
        assertFalse(viewModel.estado.value.generando)
    }

    @Test
    fun sinPerfilSeAvisaParaCompletarlo() = runTest {
        val viewModel = CrearAnuncioViewModel(
            AnuncioFalso(),
            OpcionesFalso(
                generadas = Resultado.Error(
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
        assertTrue(viewModel.estado.value.opciones.isEmpty())
    }

    @Test
    fun unErrorDeValidacionSeMuestraEnElCampoOferta() = runTest {
        val viewModel = CrearAnuncioViewModel(
            AnuncioFalso(),
            OpcionesFalso(
                generadas = Resultado.Error(
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
    fun elegirGuardaElAnuncioYMarcaLaOpcion() = runTest {
        val opciones = OpcionesFalso()
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()

        viewModel.elegir(2L)
        advanceUntilIdle()

        assertEquals(2L, opciones.idElegido)
        assertEquals("Texto de prueba", viewModel.estado.value.anuncio?.texto)
        assertEquals(2, viewModel.estado.value.opcionElegida?.numero)
        assertEquals("Guardamos tu anuncio con la imagen elegida.", viewModel.estado.value.mensajeExito)
        assertEquals(listOf(false, true, false, false), viewModel.estado.value.opciones.map { it.elegida })
        assertNull(viewModel.estado.value.eligiendoId)
    }

    @Test
    fun regenerarPideUnaNuevaVersionYComparaConLaAnterior() = runTest {
        val anuncios = AnuncioFalso()
        val viewModel = CrearAnuncioViewModel(anuncios, OpcionesFalso())
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()

        viewModel.regenerar()
        advanceUntilIdle()

        assertEquals(7L, anuncios.idRegenerado)
        // La anterior se conserva para comparar; la nueva queda pendiente de elección.
        assertEquals("Texto de prueba", viewModel.estado.value.anuncio?.texto)
        assertEquals("Texto regenerado", viewModel.estado.value.propuesta?.texto)
        assertFalse(viewModel.estado.value.regenerando)
        assertNull(viewModel.estado.value.errorRegeneracion)
    }

    @Test
    fun aceptarPropuestaActualizaElAnuncio() = runTest {
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), OpcionesFalso())
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()
        viewModel.regenerar()
        advanceUntilIdle()

        viewModel.aceptarPropuesta()
        advanceUntilIdle()

        assertEquals("Texto regenerado", viewModel.estado.value.anuncio?.texto)
        assertNull(viewModel.estado.value.propuesta)
    }

    @Test
    fun descartarPropuestaVuelveALaAnterior() = runTest {
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), OpcionesFalso())
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()
        viewModel.regenerar()
        advanceUntilIdle()

        viewModel.descartarPropuesta()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.propuesta)
        assertEquals("Texto de prueba", viewModel.estado.value.anuncio?.texto)
    }

    @Test
    fun laRegeneracionConErrorMuestraElReintento() = runTest {
        val viewModel = CrearAnuncioViewModel(
            AnuncioFalso(regenerado = Resultado.Error("La IA no respondió.")),
            OpcionesFalso()
        )
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()
        viewModel.regenerar()
        advanceUntilIdle()

        assertEquals("La IA no respondió.", viewModel.estado.value.errorRegeneracion)
        assertFalse(viewModel.estado.value.regenerando)
        assertNull(viewModel.estado.value.propuesta)
    }

    @Test
    fun retocarGuardaLaNuevaVersionYActualizaLaImagen() = runTest {
        val opciones = OpcionesFalso()
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()

        viewModel.actualizarInstruccion("Fondo más claro")
        viewModel.retocar()
        advanceUntilIdle()

        assertEquals("Fondo más claro", opciones.instruccionEnviada)
        assertEquals(2, viewModel.estado.value.historial?.versionActual)
        assertEquals("data:image/png;base64,NUEVA", viewModel.estado.value.opcionElegida?.imagen)
        assertEquals("", viewModel.estado.value.instruccion)
        assertEquals(
            "La IA retocó tu imagen. Puedes seguir ajustándola.",
            viewModel.estado.value.mensajeExito
        )
        assertFalse(viewModel.estado.value.retocando)
    }

    @Test
    fun retocarSinInstruccionNoLlamaAlBackend() = runTest {
        val opciones = OpcionesFalso()
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()

        viewModel.retocar()
        advanceUntilIdle()

        assertEquals("Escribe qué quieres cambiar de la imagen.", viewModel.estado.value.errorInstruccion)
        assertNull(opciones.instruccionEnviada)
    }

    @Test
    fun volverAUnaVersionAnteriorNoGastaOtroRetoque() = runTest {
        val opciones = OpcionesFalso(historial = Resultado.Exito(versiones(actual = 2)))
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), opciones)
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()

        viewModel.volverAVersion(1)
        advanceUntilIdle()

        assertEquals(1, opciones.versionUsada)
        assertEquals(1, viewModel.estado.value.historial?.versionActual)
        assertEquals("Ahora se usa la versión 1 de tu imagen.", viewModel.estado.value.mensajeExito)
    }

    @Test
    fun elLimiteDeRetoquesSeMuestraComoAviso() = runTest {
        val viewModel = CrearAnuncioViewModel(
            AnuncioFalso(),
            OpcionesFalso(
                retocada = Resultado.Error(
                    mensaje = "Llegaste al límite de retoques del mes.",
                    codigo = 400,
                    codigoError = "LIMITE_RETOQUES"
                )
            )
        )
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
        advanceUntilIdle()

        viewModel.actualizarInstruccion("Cambia el color")
        viewModel.retocar()
        advanceUntilIdle()

        assertEquals("Llegaste al límite de retoques del mes.", viewModel.estado.value.aviso)
        assertFalse(viewModel.estado.value.retocando)
    }

    @Test
    fun copiarMarcaElAnuncioComoCopiado() = runTest {
        val viewModel = CrearAnuncioViewModel(AnuncioFalso(), OpcionesFalso())
        viewModel.actualizarOferta("Oferta válida")
        viewModel.generar()
        advanceUntilIdle()
        viewModel.elegir(1L)
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

        fun opcion(
            numero: Int,
            elegida: Boolean = false
        ) = OpcionAnuncio(
            id = numero.toLong(),
            numero = numero,
            estilo = "Directo",
            texto = "Propuesta $numero",
            imagen = "data:image/svg+xml;base64,AAA",
            proveedorImagen = "simulado",
            imagenSimulada = true,
            textoSimulado = true,
            elegida = elegida
        )

        fun opciones() = OpcionesAnuncio(
            generacionId = 1L,
            oferta = "Oferta",
            redSocial = RedSocial.INSTAGRAM,
            opciones = (1..4).map { opcion(it) }
        )

        fun versiones(actual: Int, restantes: Int? = 19) = VersionesAnuncio(
            opcionId = 1L,
            versionActual = actual,
            imagenActual = if (actual == 2) "data:image/png;base64,NUEVA" else "data:image/svg+xml;base64,AAA",
            retoquesRestantes = restantes,
            versiones = (1..actual).map { numero ->
                VersionAnuncio(
                    numero = numero,
                    instruccion = if (numero == 1) "" else "Fondo más claro",
                    imagen = if (numero == 2) "data:image/png;base64,NUEVA" else "data:image/svg+xml;base64,AAA",
                    proveedorImagen = "simulado",
                    imagenSimulada = numero == 1,
                    actual = numero == actual
                )
            }
        )
    }
}
