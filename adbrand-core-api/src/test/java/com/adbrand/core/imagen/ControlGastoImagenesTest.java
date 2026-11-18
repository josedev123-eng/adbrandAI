package com.adbrand.core.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.repository.ConsumoImagenRepository;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// Control de gasto de imágenes: anota cada imagen y limita los retoques de pago por mes (hora de Lima).
class ControlGastoImagenesTest {

    // 15 de noviembre de 2026, 10:00 en Lima.
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneId.of("America/Lima"));

    private ConsumoImagenRepository consumos;
    private ControlGastoImagenes control;

    @BeforeEach
    void preparar() {
        consumos = mock(ConsumoImagenRepository.class);
        control = new ControlGastoImagenes(consumos, 20, RELOJ);
    }

    @Test
    void cuentaLosRetoquesRealesDesdeElPrimeroDelMesEnHoraDeLima() {
        when(consumos.countByUsuarioIdAndTipoAndSimuladoFalseAndFechaGreaterThanEqual(eq(1L), eq(ConsumoImagen.Tipo.RETOQUE), any()))
                .thenReturn(5L);

        assertThat(control.retoquesRestantes(1L)).isEqualTo(15);

        ArgumentCaptor<OffsetDateTime> desde = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(consumos).countByUsuarioIdAndTipoAndSimuladoFalseAndFechaGreaterThanEqual(eq(1L), eq(ConsumoImagen.Tipo.RETOQUE), desde.capture());
        assertThat(desde.getValue()).isEqualTo(OffsetDateTime.parse("2026-11-01T00:00:00-05:00"));
    }

    @Test
    void conRetoquesDisponiblesDejaSeguir() {
        when(consumos.countByUsuarioIdAndTipoAndSimuladoFalseAndFechaGreaterThanEqual(any(), any(), any())).thenReturn(19L);

        assertThatCode(() -> control.verificarRetoqueDisponible(1L)).doesNotThrowAnyException();
    }

    @Test
    void alLlegarAlLimiteDetieneConUnMensajeClaro() {
        when(consumos.countByUsuarioIdAndTipoAndSimuladoFalseAndFechaGreaterThanEqual(any(), any(), any())).thenReturn(20L);

        assertThatThrownBy(() -> control.verificarRetoqueDisponible(1L))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("límite de 20 retoques");
        assertThat(control.retoquesRestantes(1L)).isZero();
    }

    @Test
    void anotaCadaImagenConSuIaYSiEsDePrueba() {
        control.registrar(1L, ConsumoImagen.Tipo.BORRADOR, new ImagenGenerada("image/jpeg", "AAA", "cloudflare", false));

        ArgumentCaptor<ConsumoImagen> guardado = ArgumentCaptor.forClass(ConsumoImagen.class);
        verify(consumos).save(guardado.capture());
        assertThat(guardado.getValue().getUsuarioId()).isEqualTo(1L);
        assertThat(guardado.getValue().getTipo()).isEqualTo(ConsumoImagen.Tipo.BORRADOR);
        assertThat(guardado.getValue().getProveedor()).isEqualTo("cloudflare");
        assertThat(guardado.getValue().isSimulado()).isFalse();
    }

    @Test
    void siNoSePuedeAnotarLaImagenIgualSeEntrega() {
        when(consumos.save(any())).thenThrow(new IllegalStateException("base caída"));

        assertThatCode(() -> control.registrar(1L, ConsumoImagen.Tipo.RETOQUE,
                new ImagenGenerada("image/png", "BBB", "openrouter", false))).doesNotThrowAnyException();
    }
}
