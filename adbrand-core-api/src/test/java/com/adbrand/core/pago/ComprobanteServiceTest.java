package com.adbrand.core.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.notificacion.service.ServicioCorreo;
import com.adbrand.core.pago.dto.ComprobanteResponse;
import com.adbrand.core.pago.entity.EstadoPago;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.repository.PagoRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.service.ComprobanteService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// HU 28: el comprobante se genera una sola vez y el correo sale solo la primera vez.
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    @Mock
    private PagoRepository pagos;

    @Mock
    private PlanRepository planes;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private ServicioCorreo correo;

    private ComprobanteService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ComprobanteService(pagos, planes, usuarios, correo);
    }

    private Pago pago(String numeroComprobante) {
        Pago pago = new Pago();
        ReflectionTestUtils.setField(pago, "id", 5L);
        pago.setUsuarioId(1L);
        pago.setPlanId(2L);
        pago.setMonto(new BigDecimal("59.90"));
        pago.setFechaPago(LocalDate.of(2026, 10, 25));
        pago.setMetodo(MetodoPago.TARJETA);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setReferencia("OP-ABC123");
        pago.setNumeroComprobante(numeroComprobante);
        return pago;
    }

    private Plan plan() {
        Plan plan = new Plan();
        ReflectionTestUtils.setField(plan, "id", 2L);
        plan.setNombre("Pro");
        plan.setPrecio(new BigDecimal("59.90"));
        plan.setActivo(true);
        return plan;
    }

    private Usuario usuario() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getNombreEmpresa()).thenReturn("Dulce Perú");
        when(usuario.getCorreo()).thenReturn("ana@dulceperu.pe");
        return usuario;
    }

    @Test
    void generaElNumeroYEnviaElCorreoLaPrimeraVez() {
        Usuario usuario = usuario();
        when(pagos.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(pago(null)));
        when(planes.findById(2L)).thenReturn(Optional.of(plan()));
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        ComprobanteResponse respuesta = servicio.obtener(1L, 5L);

        assertThat(respuesta.numeroComprobante()).isEqualTo("ADB-000005");
        assertThat(respuesta.pagoId()).isEqualTo(5L);
        assertThat(respuesta.cliente()).isEqualTo("Dulce Perú");
        assertThat(respuesta.plan()).isEqualTo("Pro");
        assertThat(respuesta.monto()).isEqualByComparingTo("59.90");
        assertThat(respuesta.metodo()).isEqualTo("TARJETA");
        assertThat(respuesta.correoEnviado()).isTrue();

        verify(correo).enviar(eq("ana@dulceperu.pe"),
                contains("ADB-000005"),
                contains("Dulce Perú"));
    }

    @Test
    void siYaTieneNumeroNoVuelveAEnviarElCorreo() {
        Usuario usuario = usuario();
        when(pagos.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(pago("ADB-000005")));
        when(planes.findById(2L)).thenReturn(Optional.of(plan()));
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));

        ComprobanteResponse respuesta = servicio.obtener(1L, 5L);

        assertThat(respuesta.numeroComprobante()).isEqualTo("ADB-000005");
        assertThat(respuesta.correoEnviado()).isFalse();
        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void siElCorreoFallaElComprobanteIgualSeDevuelve() {
        Usuario usuario = usuario();
        when(pagos.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(pago(null)));
        when(planes.findById(2L)).thenReturn(Optional.of(plan()));
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        org.mockito.Mockito.doThrow(new IllegalStateException("sin SMTP"))
                .when(correo).enviar(anyString(), anyString(), anyString());

        ComprobanteResponse respuesta = servicio.obtener(1L, 5L);

        assertThat(respuesta.numeroComprobante()).isEqualTo("ADB-000005");
        assertThat(respuesta.correoEnviado()).isFalse();
    }

    @Test
    void unPagoAjenoResponde404() {
        when(pagos.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(1L, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no es tuyo");

        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }
}
