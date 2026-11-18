package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.OpcionElegidaResponse;
import com.adbrand.core.contenido.dto.OpcionesAnuncioResponse;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.entity.GeneracionAnuncio;
import com.adbrand.core.contenido.entity.OpcionAnuncio;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.repository.GeneracionAnuncioRepository;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.contenido.service.AnuncioService;
import com.adbrand.core.contenido.service.OpcionesAnuncioService;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.GeneradorImagen;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import com.adbrand.core.imagen.service.GeneradorImagenSimulado;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.service.ServicioImagenes;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.revision.dto.ResultadoRevision;
import com.adbrand.core.revision.service.FiltroContenido;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

// "Generar 4 opciones → Elegir una": textos de la IA, imágenes borrador y guardado al elegir.
class OpcionesAnuncioServiceTest {

    private PerfilNegocioService perfilService;
    private ServicioIa ia;
    private ContenidoRepository contenidos;
    private KitMarcaRepository kits;
    private GeneracionAnuncioRepository generaciones;
    private OpcionAnuncioRepository opciones;
    private ControlGastoImagenes gasto;
    private OpcionesAnuncioService servicio;
    private final List<OpcionAnuncio> guardadas = new ArrayList<>();
    // Lo que se le pidió a la "IA de imágenes" (un Cloudflare falso que solo anota la descripción).
    private final List<String> descripciones = new ArrayList<>();

    @BeforeEach
    void preparar() {
        perfilService = mock(PerfilNegocioService.class);
        ia = mock(ServicioIa.class);
        FiltroContenido filtro = mock(FiltroContenido.class);
        contenidos = mock(ContenidoRepository.class);
        kits = mock(KitMarcaRepository.class);
        generaciones = mock(GeneracionAnuncioRepository.class);
        opciones = mock(OpcionAnuncioRepository.class);

        PlantillaPromptAnuncio plantilla = new PlantillaPromptAnuncio();
        AnuncioService anuncios = new AnuncioService(perfilService, plantilla, ia, filtro, contenidos, kits);
        GeneradorImagenSimulado simulado = new GeneradorImagenSimulado();
        GeneradorImagen cloudflareFalso = new GeneradorImagen() {
            @Override
            public String nombre() {
                return ServicioImagenes.CLOUDFLARE;
            }

            @Override
            public ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase) {
                descripciones.add(descripcion);
                return new ImagenGenerada("image/jpeg", "QUJD", ServicioImagenes.CLOUDFLARE, false);
            }
        };
        ServicioImagenes imagenes = new ServicioImagenes(
                new ImagenProperties(false, "cuenta", "token", "flux", "", "gemini", 90),
                List.of(simulado, cloudflareFalso), simulado);
        gasto = mock(ControlGastoImagenes.class);
        servicio = new OpcionesAnuncioService(perfilService, plantilla, ia, imagenes, anuncios, kits, generaciones, opciones, gasto);

        when(perfilService.obtener(1L)).thenReturn(new PerfilNegocioResponse(
                1L, "Panadería Doña Rosa", "Panadería", "Familias del barrio", Tono.CERCANO, null, null));
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.empty());
        when(filtro.revisar(anyString())).thenReturn(ResultadoRevision.sinObservaciones());
        when(contenidos.save(any(Contenido.class))).thenAnswer(invocacion -> {
            Contenido contenido = invocacion.getArgument(0);
            ReflectionTestUtils.setField(contenido, "id", 50L);
            return contenido;
        });
        when(generaciones.save(any(GeneracionAnuncio.class))).thenAnswer(invocacion -> {
            GeneracionAnuncio generacion = invocacion.getArgument(0);
            ReflectionTestUtils.setField(generacion, "id", 7L);
            return generacion;
        });
        when(opciones.save(any(OpcionAnuncio.class))).thenAnswer(invocacion -> {
            OpcionAnuncio opcion = invocacion.getArgument(0);
            if (opcion.getId() == null) {
                ReflectionTestUtils.setField(opcion, "id", 100L + opcion.getNumero());
                guardadas.add(opcion);
            }
            return opcion;
        });
    }

    private static GenerarAnuncioRequest pedido() {
        return new GenerarAnuncioRequest("2x1 en pan de yema este sábado", RedSocial.INSTAGRAM);
    }

    @Test
    void generaCuatroOpcionesConUnaSolaLlamadaALaIaDeTexto() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("```json\n{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}\n```");

        OpcionesAnuncioResponse respuesta = servicio.generar(1L, pedido());

        assertThat(respuesta.generacionId()).isEqualTo(7L);
        assertThat(respuesta.opciones()).hasSize(4);
        assertThat(respuesta.opciones()).extracting("texto").containsExactly("Uno", "Dos", "Tres", "Cuatro");
        assertThat(respuesta.opciones()).extracting("estilo")
                .containsExactly("Foto realista", "Ilustración", "Minimalista", "Colorido");
        assertThat(respuesta.opciones()).allSatisfy(opcion -> {
            assertThat(opcion.imagen()).isEqualTo("data:image/jpeg;base64,QUJD");
            assertThat(opcion.proveedorImagen()).isEqualTo("cloudflare");
            assertThat(opcion.imagenSimulada()).isFalse();
        });
        verify(ia, times(1)).generarTexto(anyString(), anyString(), any(), any());
    }

    @Test
    void siLaIaNoDevuelveJsonPideLosTextosDeUnoEnUno() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("Esto no es JSON", "Texto A", "Texto B", "Texto C", "Texto D");

        OpcionesAnuncioResponse respuesta = servicio.generar(1L, pedido());

        assertThat(respuesta.opciones()).extracting("texto").containsExactly("Texto A", "Texto B", "Texto C", "Texto D");
        verify(ia, times(5)).generarTexto(anyString(), anyString(), any(), any());
    }

    @Test
    void cadaImagenTieneSuEstiloYPideQueNoLleveLetras() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");

        servicio.generar(1L, pedido());

        assertThat(descripciones).hasSize(4);
        assertThat(descripciones.get(0)).contains("realistic").contains("Main subject: pan de yema.")
                .contains("absolutely no text").doesNotContain("2x1").doesNotContain("vibrant");
        assertThat(descripciones.get(3)).contains("vibrant");
    }

    @Test
    void laDescripcionDeLaImagenUsaLosColoresDelKit() {
        KitMarca kit = new KitMarca();
        kit.setPaletaColores("#d97724 - Ámbar horneado\n#6E3B1E - Marrón tostado");
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.of(kit));
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");

        servicio.generar(1L, pedido());

        assertThat(descripciones.get(0)).contains("Color palette: #D97724, #6E3B1E.");
    }

    @Test
    void elegirUnaOpcionLaGuardaComoAnuncioYLaMarca() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");
        servicio.generar(1L, pedido());
        OpcionAnuncio segunda = guardadas.get(1);
        when(opciones.findByIdAndGeneracionUsuarioId(102L, 1L)).thenReturn(Optional.of(segunda));
        when(opciones.findByGeneracionIdOrderByNumero(7L)).thenReturn(guardadas);

        OpcionElegidaResponse elegida = servicio.elegir(1L, 102L);

        ArgumentCaptor<Contenido> captor = ArgumentCaptor.forClass(Contenido.class);
        verify(contenidos).save(captor.capture());
        assertThat(captor.getValue().getTexto()).isEqualTo("Dos");
        assertThat(captor.getValue().getOferta()).isEqualTo("2x1 en pan de yema este sábado");
        assertThat(elegida.anuncio().id()).isEqualTo(50L);
        assertThat(elegida.anuncio().estado()).isEqualTo(EstadoContenido.APROBADO);
        assertThat(elegida.opcion().elegida()).isTrue();
        assertThat(segunda.getContenidoId()).isEqualTo(50L);
        assertThat(guardadas.get(0).isElegida()).isFalse();
    }

    @Test
    void laOpcionDeOtraEmpresaRespondeNoEncontrada() {
        when(opciones.findByIdAndGeneracionUsuarioId(102L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.elegir(2L, 102L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }

    // Si la IA de imágenes ve cifras o precios intenta escribirlos (y salen letras deformes): se quitan.
    @Test
    void laDescripcionDeLaImagenNoLlevaCifrasNiPrecios() {
        PerfilNegocioResponse perfil = new PerfilNegocioResponse(
                1L, "Panadería Doña Rosa", "Panadería", "Familias", Tono.CERCANO, null, null);

        String descripcion = OpcionesAnuncioService.descripcionDeImagen(perfil, OpcionesAnuncioService.temaSinCifras(
                "2 panetones por S/ 35 con 10% de descuento hasta el 24 de diciembre"), "minimalist", List.of());

        assertThat(descripcion).doesNotContainPattern("\\d").doesNotContain("S/").doesNotContain("descuento")
                .contains("Main subject: panetones.").doesNotContain("diciembre").contains("Panadería");
    }

    // La IA de texto escribe, junto a cada versión, la escena de la imagen en inglés; esa escena es la que se dibuja.
    @Test
    void laImagenUsaLaEscenaEnInglesQueEscribioLaIaDeTexto() {
        when(ia.generarTexto(anyString(), anyString(), any(), any())).thenReturn("""
                {"opciones": [
                  {"texto": "Uno", "escena": "a golden sweet bread loaf on a rustic wooden table"},
                  {"texto": "Dos", "escena": "sweet buns in a wicker basket"},
                  {"texto": "Tres"},
                  "Cuatro"]}""");

        OpcionesAnuncioResponse respuesta = servicio.generar(1L, pedido());

        assertThat(respuesta.opciones()).extracting("texto").containsExactly("Uno", "Dos", "Tres", "Cuatro");
        assertThat(descripciones.get(0)).contains("Main subject: a golden sweet bread loaf on a rustic wooden table.");
        assertThat(descripciones.get(1)).contains("Main subject: sweet buns in a wicker basket.");
        // Sin escena se usa el producto de la oferta.
        assertThat(descripciones.get(2)).contains("Main subject: pan de yema.");
        assertThat(descripciones.get(3)).contains("Main subject: pan de yema.");
    }

    // Control de gasto: cada una de las 4 imágenes borrador queda anotada para el panel del admin.
    @Test
    void cadaBorradorQuedaAnotadoEnElControlDeGasto() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");

        servicio.generar(1L, pedido());

        verify(gasto, times(4)).registrar(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(ConsumoImagen.Tipo.BORRADOR), any());
    }
}
