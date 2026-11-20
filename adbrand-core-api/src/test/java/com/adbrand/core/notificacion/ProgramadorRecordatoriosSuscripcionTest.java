package com.adbrand.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.adbrand.core.notificacion.recordatorio.ConsultaVencimientosSuscripcion;
import com.adbrand.core.notificacion.recordatorio.ProgramadorRecordatoriosSuscripcion;
import com.adbrand.core.notificacion.recordatorio.SuscripcionPorVencer;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.entity.Suscripcion;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// HU 52: el aviso de vencimiento cambia su texto y su payload según la
// suscripción se renueve sola o la empresa tenga que pagar a mano.
@ExtendWith(MockitoExtension.class)
class ProgramadorRecordatoriosSuscripcionTest {

    @Mock
    private ConsultaVencimientosSuscripcion consulta;

    @Mock
    private ServicioNotificaciones notificaciones;

    private ProgramadorRecordatoriosSuscripcion programador;

    @BeforeEach
    void preparar() {
        programador = new ProgramadorRecordatoriosSuscripcion(consulta, notificaciones, "America/Lima");
    }

    private SuscripcionPorVencer aviso(boolean automatica, long dias) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setUsuarioId(7L);
        suscripcion.setCliente("Bodega Central");
        suscripcion.setPlan("Emprende");
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaVencimiento(LocalDate.of(2026, 11, 23));
        suscripcion.setRenovacionAutomatica(automatica);
        return new SuscripcionPorVencer(suscripcion, dias);
    }

    @Test
    void avisaLaRenovacionAutomaticaConSuPayload() {
        LocalDate hoy = LocalDate.of(2026, 11, 20);
        when(consulta.porVencer(hoy)).thenReturn(List.of(aviso(true, 3)));
        when(notificaciones.enviarAUsuario(eq(7L), anyString(), anyString(), anyMap())).thenReturn(2);

        int enviados = programador.recordarVencimientos(hoy);

        assertThat(enviados).isEqualTo(2);
        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).enviarAUsuario(eq(7L), titulo.capture(), anyString(), payload.capture());

        assertThat(titulo.getValue()).contains("se renueva");
        assertThat(payload.getValue())
                .containsEntry("type", "subscription_reminder")
                .containsEntry("plan", "Emprende")
                .containsEntry("renewal", "automatica")
                .containsEntry("days_left", "3")
                .containsEntry("expires_at", "2026-11-23");
    }

    @Test
    void laRenovacionManualPidePagarAntesDelVencimiento() {
        LocalDate hoy = LocalDate.of(2026, 11, 21);
        when(consulta.porVencer(hoy)).thenReturn(List.of(aviso(false, 2)));
        when(notificaciones.enviarAUsuario(eq(7L), anyString(), anyString(), anyMap())).thenReturn(1);

        programador.recordarVencimientos(hoy);

        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).enviarAUsuario(eq(7L), titulo.capture(), cuerpo.capture(), payload.capture());

        assertThat(titulo.getValue()).contains("vence");
        assertThat(cuerpo.getValue()).contains("Renueva antes");
        assertThat(payload.getValue()).containsEntry("renewal", "manual");
    }

    @Test
    void sinPlanesPorVencerNoEnviaNada() {
        LocalDate hoy = LocalDate.of(2026, 11, 20);
        when(consulta.porVencer(hoy)).thenReturn(List.of());

        assertThat(programador.recordarVencimientos(hoy)).isZero();
        verifyNoInteractions(notificaciones);
        verify(notificaciones, never()).enviarAUsuario(any(), anyString(), anyString(), anyMap());
    }
}
