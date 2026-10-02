package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.service.AnuncioService;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

// Tarea 4 de la HU 10: probar distintas ofertas y tonos sin llamar a la IA real.
// Mockito crea versiones "de mentira" del perfil y de la IA para controlar qué devuelven.
class AnuncioServiceTest {

    private PerfilNegocioService perfilService;
    private ServicioIa ia;
    private AnuncioService servicio;

    @BeforeEach
    void preparar() {
        perfilService = mock(PerfilNegocioService.class);
        ia = mock(ServicioIa.class);
        servicio = new AnuncioService(perfilService, new PlantillaPromptAnuncio(), ia);
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
}
