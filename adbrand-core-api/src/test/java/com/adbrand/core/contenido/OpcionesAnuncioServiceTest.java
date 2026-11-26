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
import com.adbrand.core.shared.error.SolicitudInvalidaException;
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

    // La foto del producto que subió el dueño: las 4 opciones la usan y no se gasta en imágenes de IA.
    private static final String FOTO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAE0lEQVR4nGP4z8DwnwGM/zMwAAAf7gP9NRsAMwAAAABJRU5ErkJggg==";

    @Test
    void conLaFotoDelProductoLasCuatroOpcionesLaUsanSinPedirImagenesALaIa() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");

        OpcionesAnuncioResponse respuesta = servicio.generar(1L,
                new GenerarAnuncioRequest("2x1 en pan de yema", RedSocial.INSTAGRAM, null, FOTO));

        assertThat(respuesta.opciones()).allSatisfy(opcion -> {
            assertThat(opcion.imagen()).isEqualTo(FOTO);
            assertThat(opcion.proveedorImagen()).isEqualTo("foto");
        });
        assertThat(descripciones).isEmpty();
        verify(gasto, org.mockito.Mockito.never()).registrar(any(), any(), any());
    }

    // HU 67: si el dueño pide su producto en otro ambiente, las opciones 3 y 4 lo ponen en una escena con la IA de
    // pago (con la foto como referencia) y las 1 y 2 quedan con la foto real. Solo con plan de pago y con tope.
    private final List<ImagenGenerada> basesEnviadas = new ArrayList<>();

    private OpcionesAnuncioService servicioConEscena(String origenPlan, int retoquesRestantes, boolean openRouterResponde) {
        GeneradorImagen openRouterFalso = new GeneradorImagen() {
            @Override
            public String nombre() {
                return ServicioImagenes.OPENROUTER;
            }

            @Override
            public ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase) {
                if (!openRouterResponde) {
                    throw new IllegalStateException("sin saldo");
                }
                descripciones.add(descripcion);
                basesEnviadas.add(imagenBase);
                return new ImagenGenerada("image/jpeg", "RVNDRU5B", ServicioImagenes.OPENROUTER, false);
            }
        };
        GeneradorImagen cloudflare = new GeneradorImagen() {
            @Override
            public String nombre() {
                return ServicioImagenes.CLOUDFLARE;
            }

            @Override
            public ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase) {
                return new ImagenGenerada("image/jpeg", "Q0Y=", ServicioImagenes.CLOUDFLARE, false);
            }
        };
        GeneradorImagenSimulado simulado = new GeneradorImagenSimulado();
        ServicioImagenes imagenes = new ServicioImagenes(
                new ImagenProperties(false, "cuenta", "token", "flux", "llave", "gemini", 90),
                List.of(simulado, cloudflare, openRouterFalso), simulado);
        com.adbrand.core.ia.service.LimiteConsumoIa limites = mock(com.adbrand.core.ia.service.LimiteConsumoIa.class);
        when(limites.consumoDelMes(1L)).thenReturn(new com.adbrand.core.ia.dto.ConsumoMesResponse(
                0, 400000, 0, origenPlan, "Pro", false, false));
        when(gasto.retoquesRestantes(1L)).thenReturn(retoquesRestantes);
        PlantillaPromptAnuncio plantilla = new PlantillaPromptAnuncio();
        AnuncioService anuncios = new AnuncioService(perfilService, plantilla, ia, mock(FiltroContenido.class), contenidos, kits);
        return new OpcionesAnuncioService(perfilService, plantilla, ia, imagenes, anuncios, kits, generaciones, opciones,
                gasto, limites);
    }

    private static final String JSON_ESCENA = """
            {"usoFoto": "ESCENA", "opciones": [{"texto": "Uno", "escena": "a birthday table"},
             {"texto": "Dos", "escena": "a party"}, {"texto": "Tres", "escena": "a birthday table with balloons"},
             {"texto": "Cuatro", "escena": "a kids party"}]}""";

    @Test
    void conPlanDePagoYPidiendoOtroAmbienteLasOpciones3Y4PonenElProductoEnUnaEscena() {
        when(ia.generarTexto(anyString(), anyString(), any(), any())).thenReturn(JSON_ESCENA);

        OpcionesAnuncioResponse respuesta = servicioConEscena("PLAN", 20, true).generar(1L,
                new GenerarAnuncioRequest("Mi torta en una fiesta de cumpleaños con globos", RedSocial.INSTAGRAM, null, FOTO));

        assertThat(respuesta.opciones()).extracting("imagen")
                .containsExactly(FOTO, FOTO, "data:image/jpeg;base64,RVNDRU5B", "data:image/jpeg;base64,RVNDRU5B");
        assertThat(respuesta.avisoFoto()).isNull();
        // La foto va como referencia y la escena la escribió la IA de texto.
        assertThat(basesEnviadas).hasSize(2).allSatisfy(base -> assertThat(base.dataUrl()).isEqualTo(FOTO));
        assertThat(descripciones.get(0)).contains("exact product from the reference photo").contains("balloons");
        verify(gasto, times(2)).registrar(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(ConsumoImagen.Tipo.RETOQUE), any());
    }

    @Test
    void conElPlanGratisOSinRetoquesUsaLaFotoTalCualYLoExplica() {
        when(ia.generarTexto(anyString(), anyString(), any(), any())).thenReturn(JSON_ESCENA);
        GenerarAnuncioRequest pedido = new GenerarAnuncioRequest("Mi torta en una fiesta", RedSocial.INSTAGRAM, null, FOTO);

        OpcionesAnuncioResponse gratis = servicioConEscena("GRATUITO", 20, true).generar(1L, pedido);
        assertThat(gratis.opciones()).extracting("imagen").containsOnly(FOTO);
        assertThat(gratis.avisoFoto()).contains("planes de pago");

        OpcionesAnuncioResponse sinRetoques = servicioConEscena("PLAN", 1, true).generar(1L, pedido);
        assertThat(sinRetoques.opciones()).extracting("imagen").containsOnly(FOTO);
        assertThat(sinRetoques.avisoFoto()).contains("ya usaste tus retoques");
        assertThat(basesEnviadas).isEmpty();
    }

    @Test
    void siLaIaDePagoFallaUsaLaFotoTalCual() {
        when(ia.generarTexto(anyString(), anyString(), any(), any())).thenReturn(JSON_ESCENA);

        OpcionesAnuncioResponse respuesta = servicioConEscena("PLAN", 20, false).generar(1L,
                new GenerarAnuncioRequest("Mi torta en una fiesta", RedSocial.INSTAGRAM, null, FOTO));

        assertThat(respuesta.opciones()).extracting("imagen").containsOnly(FOTO);
        assertThat(respuesta.avisoFoto()).contains("usamos tu foto tal cual");
    }

    @Test
    void conFotoLaIaDecideSiVaDeFondoOEnEscenaEnLaMismaLlamada() {
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"usoFoto\": \"FONDO\", \"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");
        ArgumentCaptor<String> sistema = ArgumentCaptor.forClass(String.class);

        OpcionesAnuncioResponse respuesta = servicioConEscena("PLAN", 20, true).generar(1L,
                new GenerarAnuncioRequest("2x1 en tortas", RedSocial.INSTAGRAM, null, FOTO));

        verify(ia).generarTexto(sistema.capture(), anyString(), any(), any());
        assertThat(sistema.getValue()).contains("\"usoFoto\": \"ESCENA\"").contains("FOTO DE SU PRODUCTO");
        assertThat(respuesta.opciones()).extracting("imagen").containsOnly(FOTO);
        assertThat(basesEnviadas).isEmpty();
    }

    @Test
    void unaFotoQueNoEsImagenSeRechazaConUnMensajeClaro() {
        assertThatThrownBy(() -> servicio.generar(1L,
                new GenerarAnuncioRequest("2x1", RedSocial.INSTAGRAM, null, "data:image/png;base64,bm9lc3VuYWltYWdlbg==")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("No pudimos abrir tu foto");
        assertThatThrownBy(() -> servicio.generar(1L,
                new GenerarAnuncioRequest("2x1", RedSocial.INSTAGRAM, null, "data:text/html;base64,PGI+")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("JPG o PNG");
    }

    // HU 67: el titular corto de cada opción se guarda y pasa al anuncio al elegirla.
    @Test
    void elTitularDeLaOpcionSeGuardaYPasaAlAnuncioAlElegirla() {
        when(ia.generarTexto(anyString(), anyString(), any(), any())).thenReturn("""
                {"opciones": [{"titular": "Tu pan, recién salido", "texto": "Uno"}, {"texto": "Dos"},
                 {"texto": "Tres"}, {"texto": "Cuatro"}]}""");

        OpcionesAnuncioResponse respuesta = servicio.generar(1L, pedido());

        assertThat(respuesta.opciones().get(0).titular()).isEqualTo("Tu pan, recién salido");
        assertThat(respuesta.opciones().get(0).angulo()).isEqualTo("Precio");
        assertThat(respuesta.opciones().get(1).titular()).isNull();
        when(opciones.findByIdAndGeneracionUsuarioId(101L, 1L)).thenReturn(Optional.of(guardadas.get(0)));
        when(opciones.findByGeneracionIdOrderByNumero(7L)).thenReturn(guardadas);
        ArgumentCaptor<Contenido> guardado = ArgumentCaptor.forClass(Contenido.class);

        servicio.elegir(1L, 101L);

        verify(contenidos).save(guardado.capture());
        assertThat(guardado.getValue().getTitular()).isEqualTo("Tu pan, recién salido");
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
    void laDescripcionDeLaImagenUsaLosColoresYElEstiloDelKit() {
        KitMarca kit = new KitMarca();
        kit.setPaletaColores("#d97724 - Ámbar horneado\n#6E3B1E - Marrón tostado");
        kit.setEstiloVisual("ARTESANAL");
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.of(kit));
        when(ia.generarTexto(anyString(), anyString(), any(), any()))
                .thenReturn("{\"opciones\": [\"Uno\", \"Dos\", \"Tres\", \"Cuatro\"]}");

        servicio.generar(1L, pedido());

        assertThat(descripciones.get(0)).contains("Brand color palette: #D97724, #6E3B1E.")
                .contains("Use these colors in the background")
                .contains("Brand mood: handcrafted, warm and rustic.");
    }

    // "Appetizing" (que dé hambre) solo para negocios de comida; para ropa o tecnología no tiene sentido.
    @Test
    void soloLosNegociosDeComidaPidenUnaImagenQueDeHambre() {
        PerfilNegocioResponse panaderia = new PerfilNegocioResponse(
                1L, "Panadería Doña Rosa", "Panadería y pastelería", "Familias", Tono.CERCANO, null, null);
        PerfilNegocioResponse tienda = new PerfilNegocioResponse(
                2L, "Moda Lima", "Ropa y calzado", "Jóvenes", Tono.CERCANO, null, null);

        assertThat(OpcionesAnuncioService.descripcionDeImagen(panaderia, "bread", "minimalist", List.of()))
                .startsWith("Appetizing");
        assertThat(OpcionesAnuncioService.descripcionDeImagen(tienda, "sneakers", "minimalist", List.of()))
                .startsWith("Eye-catching").doesNotContain("Appetizing");
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

    // HU 55: la redacción nueva de una opción elegida se genera sobre el contenido
    // que ya tenía, sin cambiar la imagen elegida.
    @Test
    void regenerarTextoDeUnaOpcionElegidaGuardaLaNuevaRedaccion() {
        OpcionAnuncio opcion = new OpcionAnuncio();
        ReflectionTestUtils.setField(opcion, "id", 102L);
        opcion.setNumero(2);
        opcion.setEstilo("Ilustración");
        opcion.setTexto("Dos");
        opcion.setContenidoId(50L);
        ReflectionTestUtils.setField(opcion, "elegida", true);
        when(opciones.findByIdAndGeneracionUsuarioId(102L, 1L)).thenReturn(Optional.of(opcion));

        Contenido original = new Contenido();
        original.setUsuarioId(1L);
        original.setTipo(Contenido.TIPO_ANUNCIO);
        original.setRedSocial(RedSocial.INSTAGRAM);
        original.setTono(Tono.CERCANO);
        original.setOferta("2x1 en pan de yema");
        original.setTexto("Dos");
        original.setEstado(EstadoContenido.APROBADO);
        ReflectionTestUtils.setField(original, "id", 50L);
        when(contenidos.findById(50L)).thenReturn(Optional.of(original));
        when(ia.generarTexto(anyString(), anyString(), any(), any())).thenReturn("Redacción nueva");

        OpcionElegidaResponse respuesta = servicio.regenerarTexto(1L, 102L);

        assertThat(respuesta.anuncio().texto()).isEqualTo("Redacción nueva");
        assertThat(respuesta.opcion().id()).isEqualTo(102L);
        assertThat(respuesta.opcion().elegida()).isTrue();
        assertThat(opcion.getContenidoId()).isEqualTo(50L);
    }

    @Test
    void regenerarTextoSinElegirLaOpcionResponde400() {
        OpcionAnuncio opcion = new OpcionAnuncio();
        ReflectionTestUtils.setField(opcion, "id", 101L);
        opcion.setNumero(1);
        opcion.setContenidoId(null);
        when(opciones.findByIdAndGeneracionUsuarioId(101L, 1L)).thenReturn(Optional.of(opcion));

        assertThatThrownBy(() -> servicio.regenerarTexto(1L, 101L))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("Primero elige");
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
