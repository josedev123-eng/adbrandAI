package com.adbrand.core.brandkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.dto.LogoDiseno;
import com.adbrand.core.brandkit.dto.LogoDiseno.FormaLogo;
import com.adbrand.core.brandkit.dto.LogoDisenoJson;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.repository.KitMarcaVersionRepository;
import com.adbrand.core.brandkit.service.EdicionKitMarcaService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// HU 29: reglas del logo diseñado y su historial, sin base de datos (Mockito).
class LogoDisenoTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static LogoDiseno logo(String texto, FormaLogo forma, String fondo, String color) {
        return new LogoDiseno(texto, forma, fondo, color, "Bricolage Grotesque");
    }

    private static Set<String> camposConError(LogoDiseno logo) {
        return VALIDADOR.validate(logo).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private KitMarcaRepository kits;
    private KitMarcaVersionRepository versiones;
    private EdicionKitMarcaService servicio;
    private KitMarca kit;

    @BeforeEach
    void preparar() {
        kits = mock(KitMarcaRepository.class);
        versiones = mock(KitMarcaVersionRepository.class);
        servicio = new EdicionKitMarcaService(kits, versiones);

        kit = new KitMarca();
        kit.setUsuarioId(1L);
        kit.setNombreNegocio("Panadería Doña Rosa");
        kit.setLogoConcepto("Un pan con un sol");
        kit.setTipografiaPrimaria("Bricolage Grotesque");
        kit.setTipografiaSecundaria("Plus Jakarta Sans");
        kit.setPaletaColores("#C62828 - Rojo horno");
        when(kits.findById(10L)).thenReturn(Optional.of(kit));
        when(kits.saveAndFlush(any(KitMarca.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void unLogoCompletoEsValido() {
        assertThat(camposConError(logo("DR", FormaLogo.CIRCULO, "#C62828", "#FFFFFF"))).isEmpty();
    }

    @Test
    void rechazaTextoVacioOMuyLargo() {
        assertThat(camposConError(logo("   ", FormaLogo.CIRCULO, "#C62828", "#FFFFFF"))).containsExactly("texto");
        assertThat(camposConError(logo("A".repeat(21), FormaLogo.CIRCULO, "#C62828", "#FFFFFF")))
                .containsExactly("texto");
        assertThat(camposConError(logo("A".repeat(20), FormaLogo.CIRCULO, "#C62828", "#FFFFFF"))).isEmpty();
    }

    @Test
    void rechazaColoresQueNoSonRRGGBBYFormaVacia() {
        assertThat(camposConError(logo("DR", null, "rojo", "#FFF")))
                .containsExactlyInAnyOrder("forma", "colorFondo", "colorTexto");
    }

    @Test
    void normalizaElTextoYLosColores() {
        LogoDiseno normalizado = logo("  DR  ", FormaLogo.CUADRADO, "#c62828", "#ffffff");

        assertThat(normalizado.texto()).isEqualTo("DR");
        assertThat(normalizado.colorFondo()).isEqualTo("#C62828");
        assertThat(normalizado.colorTexto()).isEqualTo("#FFFFFF");
    }

    @Test
    void elJsonGuardadoDevuelveElMismoLogo() {
        LogoDiseno original = logo("ROSA", FormaLogo.REDONDEADO, "#2E7D32", "#FFFFFF");

        String json = LogoDisenoJson.aTexto(original);

        assertThat(json).contains("\"forma\":\"REDONDEADO\"");
        assertThat(LogoDisenoJson.desdeTexto(json)).isEqualTo(original);
        assertThat(LogoDisenoJson.desdeTexto(null)).isNull();
        assertThat(LogoDisenoJson.desdeTexto("  ")).isNull();
        assertThat(LogoDisenoJson.aTexto(null)).isNull();
    }

    @Test
    void laVersionNuevaCopiaElRestoDelKitYGuardaElLogo() {
        LogoDiseno nuevo = logo("DR", FormaLogo.CIRCULO, "#C62828", "#FFFFFF");

        KitMarcaGeneradoResponse respuesta = servicio.guardarLogo(1L, 10L, nuevo);

        ArgumentCaptor<KitMarcaVersion> version = ArgumentCaptor.forClass(KitMarcaVersion.class);
        verify(versiones).save(version.capture());
        assertThat(version.getValue().getLogoConcepto()).isEqualTo("Un pan con un sol");
        assertThat(version.getValue().getPaletaColores()).isEqualTo("#C62828 - Rojo horno");
        assertThat(version.getValue().getTipografiaPrimaria()).isEqualTo("Bricolage Grotesque");
        assertThat(version.getValue().getTipografiaSecundaria()).isEqualTo("Plus Jakarta Sans");
        assertThat(LogoDisenoJson.desdeTexto(version.getValue().getLogoDiseno())).isEqualTo(nuevo);
        assertThat(respuesta.logoDiseno()).isEqualTo(nuevo);
        assertThat(respuesta.logoConcepto()).isEqualTo("Un pan con un sol");
    }

    @Test
    void elKitDeOtraEmpresaNoGuardaNingunaVersion() {
        assertThatThrownBy(() -> servicio.guardarLogo(2L, 10L, logo("DR", FormaLogo.CIRCULO, "#C62828", "#FFFFFF")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(versiones, never()).save(any(KitMarcaVersion.class));
        verify(kits, never()).saveAndFlush(any(KitMarca.class));
    }
}
