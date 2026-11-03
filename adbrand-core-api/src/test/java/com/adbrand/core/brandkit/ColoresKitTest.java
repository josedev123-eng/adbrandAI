package com.adbrand.core.brandkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.dto.EditarColoresRequest;
import com.adbrand.core.brandkit.dto.EditarKitMarcaRequest.ColorKit;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.repository.KitMarcaVersionRepository;
import com.adbrand.core.brandkit.service.EdicionKitMarcaService;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// HU 30: colores válidos e inválidos del editor de colores, sin base de datos (Mockito).
class ColoresKitTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> errores(EditarColoresRequest datos) {
        return VALIDADOR.validate(datos).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static EditarColoresRequest paleta(ColorKit... colores) {
        return new EditarColoresRequest(List.of(colores));
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
        kit.setPaletaColores("#2E7D32 - Verde");
        when(kits.findById(10L)).thenReturn(Optional.of(kit));
        when(kits.saveAndFlush(any(KitMarca.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#C62828", "#c62828", "#000000", "#FFFFFF", "#AbCdEf"})
    void aceptaColoresRRGGBB(String hex) {
        assertThat(errores(paleta(new ColorKit(hex, "Color")))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"#FFF", "C62828", "#C6282", "#C628288", "#GGGGGG", "rojo", "", "  "})
    void rechazaColoresQueNoSonRRGGBB(String hex) {
        assertThat(errores(paleta(new ColorKit(hex, "Color")))).containsExactly("colores[0].hex");
    }

    @Test
    void dicenCualColorEstaMal() {
        assertThat(errores(paleta(new ColorKit("#C62828", ""), new ColorKit("crema", ""), new ColorKit("#263238", ""))))
                .containsExactly("colores[1].hex");
    }

    @Test
    void elNombreEsOpcionalPeroNoPasaDe100Caracteres() {
        assertThat(errores(paleta(new ColorKit("#C62828", null)))).isEmpty();
        assertThat(errores(paleta(new ColorKit("#C62828", "a".repeat(100))))).isEmpty();
        assertThat(errores(paleta(new ColorKit("#C62828", "a".repeat(101))))).containsExactly("colores[0].nombre");
    }

    @Test
    void aceptaDe1A8Colores() {
        assertThat(errores(new EditarColoresRequest(List.of()))).containsExactly("colores");
        assertThat(errores(new EditarColoresRequest(Collections.nCopies(8, new ColorKit("#C62828", ""))))).isEmpty();
        assertThat(errores(new EditarColoresRequest(Collections.nCopies(9, new ColorKit("#C62828", "")))))
                .containsExactly("colores");
    }

    @Test
    void guardaLaPaletaEnMayusculasYConElMismoFormatoDeLaIa() {
        servicio.guardarColores(1L, 10L, paleta(
                new ColorKit("#c62828", "  Rojo horno - Ofertas  "),
                new ColorKit("#fff3e0", ""),
                new ColorKit("#263238", null)));

        assertThat(kit.getPaletaColores()).isEqualTo("#C62828 - Rojo horno - Ofertas\n#FFF3E0\n#263238");
        verify(versiones).save(any(KitMarcaVersion.class));
    }

    @Test
    void unColorRepetidoAunqueCambienLasMayusculasNoSeGuarda() {
        assertThatThrownBy(() -> servicio.guardarColores(1L, 10L, paleta(
                new ColorKit("#c62828", "Rojo"),
                new ColorKit("#263238", "Gris"),
                new ColorKit("#C62828", "Otro rojo"))))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El color #C62828 está repetido.");

        assertThat(kit.getPaletaColores()).isEqualTo("#2E7D32 - Verde");
        verify(versiones, never()).save(any(KitMarcaVersion.class));
        verify(kits, never()).saveAndFlush(any(KitMarca.class));
    }
}
