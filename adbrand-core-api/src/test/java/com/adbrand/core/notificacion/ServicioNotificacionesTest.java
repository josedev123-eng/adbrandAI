package com.adbrand.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.notificacion.entity.DispositivoUsuario;
import com.adbrand.core.notificacion.entity.TipoNotificacion;
import com.adbrand.core.notificacion.push.EnviadorPush;
import com.adbrand.core.notificacion.repository.DispositivoUsuarioRepository;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import com.adbrand.core.notificacion.service.ServicioPreferenciasNotificacion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// HU 50 y 51: el envío usa solo los equipos activos, limpia los tokens inválidos
// y respeta las preferencias de avisos de la empresa.
@ExtendWith(MockitoExtension.class)
class ServicioNotificacionesTest {

    @Mock
    private DispositivoUsuarioRepository dispositivos;

    @Mock
    private EnviadorPush enviador;

    @Mock
    private ServicioPreferenciasNotificacion preferencias;

    @InjectMocks
    private ServicioNotificaciones servicio;

    private DispositivoUsuario equipo(String token) {
        DispositivoUsuario dispositivo = new DispositivoUsuario();
        dispositivo.setUsuarioId(3L);
        dispositivo.setFcmToken(token);
        return dispositivo;
    }

    @Test
    void enviaSoloALosEquiposActivosDelUsuario() {
        when(dispositivos.findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(3L))
                .thenReturn(List.of(equipo("token-a"), equipo("token-b")));
        when(enviador.enviar(anyList(), any(), any(), any()))
                .thenReturn(new EnviadorPush.ResultadoEnvio(2, List.of()));

        int enviados = servicio.enviarAUsuario(3L, "Hola", "Cuerpo", Map.of("type", "system_alert"));

        assertThat(enviados).isEqualTo(2);
        verify(enviador).enviar(
                eq(List.of("token-a", "token-b")), eq("Hola"), eq("Cuerpo"),
                eq(Map.of("type", "system_alert")));
        verify(dispositivos, never()).deleteByFcmTokenIn(any());
    }

    @Test
    void borraLosTokensQueFirebaseYaNoAcepta() {
        when(dispositivos.findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(3L))
                .thenReturn(List.of(equipo("token-viejo")));
        when(enviador.enviar(anyList(), any(), any(), any()))
                .thenReturn(new EnviadorPush.ResultadoEnvio(0, List.of("token-viejo")));

        servicio.enviarAUsuario(3L, "Hola", "Cuerpo", Map.of());

        verify(dispositivos).deleteByFcmTokenIn(List.of("token-viejo"));
    }

    @Test
    void sinEquiposNoLlamaAlProveedor() {
        when(dispositivos.findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(3L))
                .thenReturn(List.of());

        assertThat(servicio.enviarAUsuario(3L, "Hola", "Cuerpo", Map.of())).isZero();
        verify(enviador, never()).enviar(anyList(), any(), any(), any());
    }

    // HU 51: una empresa que apagó los avisos de campaña no recibe el recordatorio.
    @Test
    void noEnviaCampanasSiLaEmpresaLasApago() {
        when(preferencias.permite(3L, TipoNotificacion.CAMPANA)).thenReturn(false);

        int enviados = servicio.enviarAUsuario(
                3L, "Mañana empieza tu campaña", "Revisa el calendario",
                Map.of("type", "campaign_reminder"));

        assertThat(enviados).isZero();
        verify(dispositivos, never()).findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(any());
        verify(enviador, never()).enviar(anyList(), any(), any(), any());
    }

    @Test
    void enviaSuscripcionesSiLaEmpresaLasDejoActivas() {
        when(preferencias.permite(3L, TipoNotificacion.SUSCRIPCION)).thenReturn(true);
        when(dispositivos.findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(3L))
                .thenReturn(List.of(equipo("token-a")));
        when(enviador.enviar(anyList(), any(), any(), any()))
                .thenReturn(new EnviadorPush.ResultadoEnvio(1, List.of()));

        int enviados = servicio.enviarAUsuario(
                3L, "Tu plan vence pronto", "Renueva a tiempo",
                Map.of("type", "subscription_reminder"));

        assertThat(enviados).isEqualTo(1);
        verify(preferencias).permite(3L, TipoNotificacion.SUSCRIPCION);
    }

    // Los avisos del sistema no se pueden apagar: son avisos de la cuenta.
    @Test
    void losAvisosDelSistemaSiempreLlegan() {
        when(dispositivos.findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(3L))
                .thenReturn(List.of(equipo("token-a")));
        when(enviador.enviar(anyList(), any(), any(), any()))
                .thenReturn(new EnviadorPush.ResultadoEnvio(1, List.of()));

        servicio.enviarAUsuario(3L, "Aviso", "Cuenta", Map.of("type", "system_alert"));

        verify(preferencias, never()).permite(any(), any());
    }
}
