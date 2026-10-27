package com.adbrand.core.pago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.pago.dto.OperacionYapeResponse;
import com.adbrand.core.pago.dto.PagoResponse;
import com.adbrand.core.pago.dto.RegistrarYapeRequest;
import com.adbrand.core.pago.entity.EstadoOperacionYape;
import com.adbrand.core.pago.entity.EstadoPago;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.OperacionYape;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.repository.OperacionYapeRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.service.OperacionYapeService;
import com.adbrand.core.pago.service.PagoService;
import com.adbrand.core.shared.error.ConflictoException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// HU 27: generar el QR deja una operación pendiente; confirmar crea el pago real una sola vez.
@ExtendWith(MockitoExtension.class)
class OperacionYapeServiceTest {

    @Mock
    private OperacionYapeRepository operaciones;

    @Mock
    private PlanRepository planes;

    @Mock
    private PagoService pagos;

    @Mock
    private UsuarioRepository usuarios;

    private OperacionYapeService servicio;

    @BeforeEach
    void setUp() {
        servicio = new OperacionYapeService(operaciones, planes, pagos, usuarios);
    }

    private Plan plan() {
        Plan plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 2L);
        plan.setNombre("Pro");
        plan.setPrecio(new BigDecimal("59.90"));
        plan.setLimiteTokens(150000);
        plan.setActivo(true);
        return plan;
    }

    private OperacionYape operacion(EstadoOperacionYape estado) {
        OperacionYape operacion = new OperacionYape();
        ReflectionTestUtils.setField(operacion, "id", 7L);
        operacion.setUsuarioId(1L);
        operacion.setPlanId(2L);
        operacion.setMonto(new BigDecimal("59.90"));
        operacion.setCodigo("YPE-ABC12345");
        operacion.setEstado(estado);
        return operacion;
    }

    @Test
    void registrarDejaUnaOperacionPendienteConSuQr() {
        Plan plan = plan();
        when(pagos.planActivo(2L)).thenReturn(plan);
        when(operaciones.saveAndFlush(any(OperacionYape.class))).thenAnswer(invocacion -> {
            OperacionYape guardada = invocacion.getArgument(0);
            ReflectionTestUtils.setField(guardada, "id", 7L);
            return guardada;
        });

        OperacionYapeResponse respuesta = servicio.registrar(1L, new RegistrarYapeRequest(2L, "987654321"));

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.codigo()).startsWith("YPE-");
        assertThat(respuesta.plan()).isEqualTo("Pro");
        assertThat(respuesta.monto()).isEqualByComparingTo("59.90");
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.celular()).isEqualTo("987654321");
        assertThat(respuesta.contenidoQr()).contains(respuesta.codigo()).contains("monto=59.90");
    }

    @Test
    void confirmarRegistraElPagoYMarcaLaOperacion() {
        OperacionYape pendiente = operacion(EstadoOperacionYape.PENDIENTE);
        Plan plan = plan();
        Usuario usuario = mock(Usuario.class);
        when(usuario.getNombre()).thenReturn("Ana Quispe");

        when(operaciones.findByIdAndUsuarioId(7L, 1L)).thenReturn(Optional.of(pendiente));
        when(pagos.planActivo(2L)).thenReturn(plan);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        Pago pago = new Pago();
        ReflectionTestUtils.setField(pago, "id", 5L);
        pago.setMonto(new BigDecimal("59.90"));
        pago.setFechaPago(LocalDate.of(2026, 10, 23));
        pago.setMetodo(MetodoPago.YAPE);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setReferencia("YPE-ABC12345");
        when(pagos.registrarPago(1L, plan, MetodoPago.YAPE, "YPE-ABC12345", "Ana Quispe", null))
                .thenReturn(pago);

        PagoResponse respuesta = servicio.confirmar(1L, 7L);

        assertThat(respuesta.id()).isEqualTo(5L);
        assertThat(respuesta.metodo()).isEqualTo("YAPE");
        assertThat(respuesta.estado()).isEqualTo("APROBADO");
        assertThat(pendiente.getEstado()).isEqualTo(EstadoOperacionYape.CONFIRMADO);
        assertThat(pendiente.getFechaConfirmacion()).isNotNull();
        verify(operaciones).save(pendiente);
    }

    @Test
    void confirmarUnaOperacionAjenaResponde404() {
        when(operaciones.findByIdAndUsuarioId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmar(1L, 7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");

        verify(pagos, never()).registrarPago(any(), any(), any(), any(), any(), any());
    }

    @Test
    void noSePuedeConfirmarDosVeces() {
        when(operaciones.findByIdAndUsuarioId(7L, 1L))
                .thenReturn(Optional.of(operacion(EstadoOperacionYape.CONFIRMADO)));

        assertThatThrownBy(() -> servicio.confirmar(1L, 7L))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya fue confirmada");

        verify(pagos, never()).registrarPago(any(), any(), any(), any(), any(), any());
    }

    @Test
    void pendientesDevuelveSoloLasOperacionesDelUsuario() {
        when(operaciones.findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(
                eq(1L), eq(EstadoOperacionYape.PENDIENTE)))
                .thenReturn(List.of(operacion(EstadoOperacionYape.PENDIENTE)));
        when(planes.findById(2L)).thenReturn(Optional.of(plan()));

        List<OperacionYapeResponse> pendientes = servicio.pendientes(1L);

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).plan()).isEqualTo("Pro");
        assertThat(pendientes.get(0).estado()).isEqualTo("PENDIENTE");
    }
}
