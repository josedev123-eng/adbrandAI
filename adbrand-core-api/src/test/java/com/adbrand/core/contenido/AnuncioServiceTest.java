package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.service.AnuncioService;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.revision.dto.ResultadoRevision;
import com.adbrand.core.revision.service.FiltroContenido;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

// Tarea 4 de la HU 10: probar distintas ofertas y tonos sin llamar a la IA real.
// Mockito crea versiones "de mentira" del perfil y de la IA para controlar qué devuelven.
class AnuncioServiceTest {

    private PerfilNegocioService perfilService;
    private ServicioIa ia;
    private FiltroContenido filtro;
    private ContenidoRepository contenidos;
    private AnuncioService servicio;

    @BeforeEach
    void preparar() {
        perfilService = mock(PerfilNegocioService.class);
        ia = mock(ServicioIa.class);
        filtro = mock(FiltroContenido.class);
        contenidos = mock(ContenidoRepository.class);
        servicio = new AnuncioService(perfilService, new PlantillaPromptAnuncio(), ia, filtro, contenidos);

        // Por defecto el filtro aprueba y "guardar" devuelve lo mismo que recibe.
        when(filtro.revisar(anyString())).thenReturn(ResultadoRevision.sinObservaciones());
        when(contenidos.save(any(Contenido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void perfilConTono(Tono tono) {
        when(perfilService.obtener(1L)).thenReturn(new PerfilNegocioResponse(
                1L, "Panadería Doña Rosa", "Panadería y pastelería", "Familias del barrio", tono, null, null));
    }

    @ParameterizedTest(name = "{0} + \"{1}\" en {2}")
    @CsvSource(delimiter = '|', textBlock = """
            CERCANO     | 2 panetones por S/ 35 hasta el 24 de diciembre | INSTAGRAM | tutea al cliente
            PROFESIONAL | 10% de descuento en tortas por encargo          | FACEBOOK  | trata de usted
            DIVERTIDO   | Martes de pan con chicharrón a S/ 6             | INSTAGRAM | humor ligero
            ELEGANTE    | Nueva línea de postres franceses desde S/ 18    | FACEBOOK  | sin emojis
            """)
    void elPromptLlevaLaOfertaYElTonoDelPerfil(Tono tono, String oferta, RedSocial red, String instruccion) {
        perfilConTono(tono);
        when(ia.generarTexto(anyString(), anyString())).thenReturn("Texto del anuncio");

        AnuncioGeneradoResponse anuncio = servicio.generar(1L, new GenerarAnuncioRequest(oferta, red));

        ArgumentCaptor<String> promptUsuario = ArgumentCaptor.forClass(String.class);
        verify(ia).generarTexto(anyString(), promptUsuario.capture());
        assertThat(promptUsuario.getValue())
                .contains("Oferta a anunciar: " + oferta)
                .contains(instruccion);
        assertThat(anuncio.texto()).isEqualTo("Texto del anuncio");
        assertThat(anuncio.tono()).isEqualTo(tono);
        assertThat(anuncio.redSocial()).isEqualTo(red);
        assertThat(anuncio.estado()).isEqualTo(EstadoContenido.APROBADO);
    }

    @Test
    void sinPerfilNoSeLlamaALaIa() {
        when(perfilService.obtener(1L)).thenThrow(new RecursoNoEncontradoException("PERFIL_NO_ENCONTRADO", "Sin perfil"));

        assertThatThrownBy(() -> servicio.generar(1L, new GenerarAnuncioRequest("Promo", RedSocial.INSTAGRAM)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(ia, never()).generarTexto(anyString(), anyString());
    }

    @Test
    void siLaIaFallaElErrorLlegaAlController() {
        perfilConTono(Tono.CERCANO);
        when(ia.generarTexto(anyString(), anyString())).thenThrow(new IaNoDisponibleException("caída", null));

        assertThatThrownBy(() -> servicio.generar(1L, new GenerarAnuncioRequest("Promo", RedSocial.INSTAGRAM)))
                .isInstanceOf(IaNoDisponibleException.class);
    }

    // HU 13, criterio 1: el anuncio que pasa el filtro se guarda APROBADO y sin motivo.
    @Test
    void elAnuncioQuePasaElFiltroSeGuardaAprobado() {
        perfilConTono(Tono.CERCANO);
        when(ia.generarTexto(anyString(), anyString())).thenReturn("2 panetones por S/ 35");

        servicio.generar(1L, new GenerarAnuncioRequest("2 panetones por S/ 35", RedSocial.INSTAGRAM));

        Contenido guardado = contenidoGuardado();
        assertThat(guardado.getEstado()).isEqualTo(EstadoContenido.APROBADO);
        assertThat(guardado.getMotivoRevision()).isNull();
        assertThat(guardado.getTexto()).isEqualTo("2 panetones por S/ 35");
        assertThat(guardado.getUsuarioId()).isEqualTo(1L);
        assertThat(guardado.getTipo()).isEqualTo(Contenido.TIPO_ANUNCIO);
        verify(filtro).revisar("2 panetones por S/ 35");
    }

    // HU 13, criterio 2: el anuncio que incumple se guarda DUDOSO con su motivo y la web lo recibe así.
    @Test
    void elAnuncioQueIncumpleSeGuardaDudosoConSuMotivo() {
        perfilConTono(Tono.CERCANO);
        when(ia.generarTexto(anyString(), anyString())).thenReturn("Cigarrillos importados a S/ 10");
        when(filtro.revisar("Cigarrillos importados a S/ 10")).thenReturn(
                new ResultadoRevision(false, List.of("\"cigarrillos\": Publicidad de tabaco.")));

        AnuncioGeneradoResponse anuncio = servicio.generar(
                1L, new GenerarAnuncioRequest("Cigarrillos importados a S/ 10", RedSocial.FACEBOOK));

        Contenido guardado = contenidoGuardado();
        assertThat(guardado.getEstado()).isEqualTo(EstadoContenido.DUDOSO);
        assertThat(guardado.getMotivoRevision()).isEqualTo("\"cigarrillos\": Publicidad de tabaco.");
        assertThat(anuncio.estado()).isEqualTo(EstadoContenido.DUDOSO);
        assertThat(anuncio.motivoRevision()).contains("Publicidad de tabaco.");
    }

    @Test
    void unMotivoMuyLargoSeRecortaA500Caracteres() {
        perfilConTono(Tono.CERCANO);
        when(ia.generarTexto(anyString(), anyString())).thenReturn("texto");
        when(filtro.revisar("texto")).thenReturn(new ResultadoRevision(false, List.of("x".repeat(800))));

        servicio.generar(1L, new GenerarAnuncioRequest("Promo", RedSocial.INSTAGRAM));

        assertThat(contenidoGuardado().getMotivoRevision()).hasSize(500).endsWith("...");
    }

    // HU 12: regenerar crea una alternativa nueva reutilizando los parámetros originales.
    @Test
    void regenerar_creaNuevaAlternativaConMismosParametros() {
        perfilConTono(Tono.CERCANO);
        when(ia.generarTexto(anyString(), anyString())).thenReturn("Versión regenerada");
        Contenido original = contenidoDe(10L, "2 panetones por S/ 35", RedSocial.INSTAGRAM);
        when(contenidos.findById(10L)).thenReturn(Optional.of(original));

        AnuncioGeneradoResponse alternativa = servicio.regenerar(1L, 10L);

        assertThat(alternativa.texto()).isEqualTo("Versión regenerada");
        assertThat(alternativa.redSocial()).isEqualTo(original.getRedSocial());
        assertThat(alternativa.tono()).isEqualTo(Tono.CERCANO);
        assertThat(alternativa.estado()).isEqualTo(EstadoContenido.APROBADO);

        Contenido guardado = contenidoGuardado();
        assertThat(guardado).isNotSameAs(original);
        assertThat(guardado.getOferta()).isEqualTo(original.getOferta());
        assertThat(guardado.getRedSocial()).isEqualTo(original.getRedSocial());
        assertThat(guardado.getUsuarioId()).isEqualTo(1L);
        assertThat(guardado.getTipo()).isEqualTo(Contenido.TIPO_ANUNCIO);
    }

    // HU 12: cada regeneración guarda una alternativa distinta de las anteriores.
    @Test
    void multiplesRegeneraciones_creanAlternativasDistintas() {
        perfilConTono(Tono.PROFESIONAL);
        when(ia.generarTexto(anyString(), anyString()))
                .thenReturn("Versión 1", "Versión 2", "Versión 3");
        Contenido original = contenidoDe(20L, "10% de descuento en tortas", RedSocial.FACEBOOK);
        when(contenidos.findById(20L)).thenReturn(Optional.of(original));

        AnuncioGeneradoResponse primera = servicio.regenerar(1L, 20L);
        AnuncioGeneradoResponse segunda = servicio.regenerar(1L, 20L);
        AnuncioGeneradoResponse tercera = servicio.regenerar(1L, 20L);

        assertThat(primera.texto()).isEqualTo("Versión 1");
        assertThat(segunda.texto()).isEqualTo("Versión 2");
        assertThat(tercera.texto()).isEqualTo("Versión 3");
        assertThat(primera.texto()).isNotEqualTo(segunda.texto());
        assertThat(segunda.texto()).isNotEqualTo(tercera.texto());

        ArgumentCaptor<Contenido> captor = ArgumentCaptor.forClass(Contenido.class);
        verify(contenidos, times(3)).save(captor.capture());
        List<Contenido> guardados = captor.getAllValues();
        assertThat(guardados.get(0)).isNotSameAs(guardados.get(1));
        assertThat(guardados.get(1)).isNotSameAs(guardados.get(2));
        for (Contenido guardado : guardados) {
            assertThat(guardado.getOferta()).isEqualTo(original.getOferta());
            assertThat(guardado.getRedSocial()).isEqualTo(original.getRedSocial());
            assertThat(guardado.getUsuarioId()).isEqualTo(1L);
        }
    }

    private Contenido contenidoDe(Long id, String oferta, RedSocial red) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(1L);
        contenido.setTipo(Contenido.TIPO_ANUNCIO);
        contenido.setRedSocial(red);
        contenido.setTono(Tono.CERCANO);
        contenido.setOferta(oferta);
        contenido.setTexto("Versión original");
        contenido.setEstado(EstadoContenido.APROBADO);
        org.springframework.test.util.ReflectionTestUtils.setField(contenido, "id", id);
        return contenido;
    }

    private Contenido contenidoGuardado() {
        ArgumentCaptor<Contenido> captor = ArgumentCaptor.forClass(Contenido.class);
        verify(contenidos).save(captor.capture());
        return captor.getValue();
    }
}
