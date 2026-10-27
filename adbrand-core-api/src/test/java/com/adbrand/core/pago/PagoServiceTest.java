package com.adbrand.core.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.PagoTarjetaRequest;
import com.adbrand.core.pago.entity.EstadoPago;
import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.entity.Suscripcion;
import com.adbrand.core.pago.repository.PagoRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.repository.SuscripcionRepository;
import com.adbrand.core.pago.service.PagoService;
import com.adbrand.core.pago.service.PasarelaSimulada;
import com.adbrand.core.shared.error.ConflictoException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// HU 26: reglas del pago con tarjeta (aprobado, rechazado, plan inexistente y plan ya activo).
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PlanRepository planes;

    @Mock
    private SuscripcionRepository suscripciones;

    @Mock
    private PagoRepository pagos;

    @Mock
    private UsuarioRepository usuarios;

    private PagoService servicio;

    @BeforeEach
    void setUp() {
        servicio = new PagoService(planes, suscripciones, pagos, new PasarelaSimulada(), usuarios);
    }

    private Plan plan() {
        Plan plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 1L);
        plan.setNombre("Pro");
        plan.setPrecio(new BigDecimal("59.90"));
        plan.setLimiteTokens(150000);
        plan.setActivo(true);
        return plan;
    }

    private PagoTarjetaRequest request(String numero) {
        return new PagoTarjetaRequest(1L, numero, "Ana Quispe", "12/30", "123");
    }

    private void sinSuscripcionActiva() {
        when(suscripciones.existsByUsuarioIdAndPlanAndEstadoAndFechaVencimientoGreaterThanEqual(
                eq(1L), anyString(), eq(EstadoSuscripcion.ACTIVA), any(LocalDate.class))).thenReturn(false);
    }

    private void guardaYDevuelveIds() {
        when(usuarios.findById(1L)).thenReturn(Optional.empty());
        when(suscripciones.saveAndFlush(any(Suscripcion.class))).thenAnswer(invocacion -> {
            Suscripcion suscripcion = invocacion.getArgument(0);
            ReflectionTestUtils.setField(suscripcion, "id", 9L);
            return suscripcion;
        });
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            ReflectionTestUtils.setField(pago, "id", 5L);
            return pago;
        });
    }

    @Test
    void apruebaElPagoYCreaLaSuscripcion() {
        when(planes.findById(1L)).thenReturn(Optional.of(plan()));
        sinSuscripcionActiva();
        guardaYDevuelveIds();

        PagoResponse respuesta = servicio.pagarConTarjeta(1L, request("4111 1111 1111 1111"));

        assertThat(respuesta.id()).isEqualTo(5L);
        assertThat(respuesta.plan()).isEqualTo("Pro");
        assertThat(respuesta.monto()).isEqualByComparingTo("59.90");
        assertThat(respuesta.metodo()).isEqualTo("TARJETA");
        assertThat(respuesta.estado()).isEqualTo("APROBADO");
        assertThat(respuesta.referencia()).startsWith("OP-");

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagos).saveAndFlush(captor.capture());
        Pago guardado = captor.getValue();
        assertThat(guardado.getSuscripcionId()).isEqualTo(9L);
        assertThat(guardado.getUsuarioId()).isEqualTo(1L);
        assertThat(guardado.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(guardado.getMetodo()).isEqualTo(MetodoPago.TARJETA);
        assertThat(guardado.getUltimosDigitos()).isEqualTo("1111");
    }

    @Test
    void rechazaLaTarjetaYNoGuardaNada() {
        when(planes.findById(1L)).thenReturn(Optional.of(plan()));
        sinSuscripcionActiva();

        assertThatThrownBy(() -> servicio.pagarConTarjeta(1L, request("4000 0000 0000 0002")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("rechazada por el banco");

        verify(pagos, never()).saveAndFlush(any());
    }

    @Test
    void noCobraDosVecesElMismoPlanActivo() {
        when(planes.findById(1L)).thenReturn(Optional.of(plan()));
        when(suscripciones.existsByUsuarioIdAndPlanAndEstadoAndFechaVencimientoGreaterThanEqual(
                eq(1L), eq("Pro"), eq(EstadoSuscripcion.ACTIVA), any(LocalDate.class))).thenReturn(true);

        assertThatThrownBy(() -> servicio.pagarConTarjeta(1L, request("4111 1111 1111 1111")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("suscripción activa");

        verify(pagos, never()).saveAndFlush(any());
    }

    @Test
    void siElPlanNoExisteResponde404() {
        when(planes.findById(99L)).thenReturn(Optional.empty());
        PagoTarjetaRequest datos = new PagoTarjetaRequest(99L, "4111 1111 1111 1111", "Ana Quispe", "12/30", "123");

        assertThatThrownBy(() -> servicio.pagarConTarjeta(1L, datos))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }
}
