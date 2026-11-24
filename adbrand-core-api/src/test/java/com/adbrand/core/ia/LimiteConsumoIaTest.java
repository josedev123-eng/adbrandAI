package com.adbrand.core.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.ia.dto.ConsumoMesResponse;
import com.adbrand.core.ia.entity.LimiteTokens;
import com.adbrand.core.ia.repository.ConsumoTokenRepository;
import com.adbrand.core.ia.repository.LimiteTokensRepository;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.entity.Suscripcion;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.repository.SuscripcionRepository;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// El límite mensual de tokens: personalizado por el admin, el del plan pagado o el plan gratuito.
class LimiteConsumoIaTest {

    // 15 de noviembre de 2026, 10:00 en Lima.
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneId.of("America/Lima"));

    private ConsumoTokenRepository consumos;
    private LimiteTokensRepository limites;
    private SuscripcionRepository suscripciones;
    private PlanRepository planes;
    private LimiteConsumoIa servicio;

    @BeforeEach
    void preparar() {
        consumos = mock(ConsumoTokenRepository.class);
        limites = mock(LimiteTokensRepository.class);
        suscripciones = mock(SuscripcionRepository.class);
        planes = mock(PlanRepository.class);
        servicio = new LimiteConsumoIa(consumos, limites, suscripciones, planes, RELOJ);

        when(limites.findFirstByUsuarioIdIsNull()).thenReturn(Optional.of(limite(null, 10_000)));
        when(limites.findByUsuarioId(any())).thenReturn(Optional.empty());
        when(suscripciones.findFirstByUsuarioIdAndEstadoAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDescIdDesc(
                any(), any(), any())).thenReturn(Optional.empty());
    }

    private static LimiteTokens limite(Long usuarioId, long tokens) {
        LimiteTokens limite = new LimiteTokens();
        limite.setUsuarioId(usuarioId);
        limite.setLimiteMensual(tokens);
        limite.setPorcentajeAviso(80);
        return limite;
    }

    private void conPlanActivo(String nombre, int tokens) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setPlan(nombre);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        when(suscripciones.findFirstByUsuarioIdAndEstadoAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoDescIdDesc(
                eq(1L), eq(EstadoSuscripcion.ACTIVA), eq(LocalDate.of(2026, 11, 15)))).thenReturn(Optional.of(suscripcion));
        Plan plan = new Plan();
        plan.setNombre(nombre);
        plan.setLimiteTokens(tokens);
        when(planes.findFirstByNombre(nombre)).thenReturn(Optional.of(plan));
    }

    private void usados(long tokens) {
        when(consumos.sumarTokensDesde(eq(1L), any())).thenReturn(tokens);
    }

    @Test
    void sinPlanPagadoUsaElPlanGratuito() {
        usados(2_500);

        ConsumoMesResponse consumo = servicio.consumoDelMes(1L);

        assertThat(consumo.origen()).isEqualTo("GRATUITO");
        assertThat(consumo.limite()).isEqualTo(10_000);
        assertThat(consumo.porcentaje()).isEqualTo(25);
        assertThat(consumo.agotado()).isFalse();
    }

    @Test
    void conPlanActivoUsaElLimiteDelPlan() {
        conPlanActivo("Pro", 150_000);
        usados(130_000);

        ConsumoMesResponse consumo = servicio.consumoDelMes(1L);

        assertThat(consumo.origen()).isEqualTo("PLAN");
        assertThat(consumo.plan()).isEqualTo("Pro");
        assertThat(consumo.limite()).isEqualTo(150_000);
        assertThat(consumo.enAlerta()).isTrue();
    }

    @Test
    void elLimitePropioDelAdministradorTieneLaUltimaPalabra() {
        conPlanActivo("Pro", 150_000);
        when(limites.findByUsuarioId(1L)).thenReturn(Optional.of(limite(1L, 5_000)));
        usados(100);

        assertThat(servicio.consumoDelMes(1L).origen()).isEqualTo("PERSONALIZADO");
        assertThat(servicio.consumoDelMes(1L).limite()).isEqualTo(5_000);
    }

    @Test
    void cuentaDesdeElPrimeroDelMesEnHoraDeLima() {
        usados(0);

        servicio.consumoDelMes(1L);

        ArgumentCaptor<OffsetDateTime> desde = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(consumos).sumarTokensDesde(eq(1L), desde.capture());
        assertThat(desde.getValue()).isEqualTo(OffsetDateTime.parse("2026-11-01T00:00:00-05:00"));
    }

    @Test
    void conCupoDisponibleDejaUsarLaIa() {
        usados(9_999);

        assertThatCode(() -> servicio.verificarDisponible(1L)).doesNotThrowAnyException();
    }

    @Test
    void alAgotarElPlanGratuitoPideElegirUnPlan() {
        usados(10_000);

        assertThatThrownBy(() -> servicio.verificarDisponible(1L))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("plan gratuito")
                .hasMessageContaining("10 000")
                .extracting("codigo").isEqualTo("LIMITE_TOKENS");
    }

    @Test
    void alAgotarUnPlanPagadoPideMejorarlo() {
        conPlanActivo("Básico", 50_000);
        usados(51_000);

        assertThatThrownBy(() -> servicio.verificarDisponible(1L))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("Mejora tu plan");
    }
}
