package com.adbrand.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.notificacion.dto.PreferenciaNotificacionRequest;
import com.adbrand.core.notificacion.dto.PreferenciaNotificacionResponse;
import com.adbrand.core.notificacion.entity.PreferenciaNotificacion;
import com.adbrand.core.notificacion.entity.TipoNotificacion;
import com.adbrand.core.notificacion.repository.PreferenciaNotificacionRepository;
import com.adbrand.core.notificacion.service.ServicioPreferenciasNotificacion;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// HU 51: sin fila guardada todo está activo; un cambio parcial respeta los otros
// interruptores y los avisos apagados no se envían.
@ExtendWith(MockitoExtension.class)
class ServicioPreferenciasNotificacionTest {

    @Mock
    private PreferenciaNotificacionRepository repositorio;

    @InjectMocks
    private ServicioPreferenciasNotificacion servicio;

    @Test
    void sinPreferenciasGuardadasTodoEstaActivo() {
        when(repositorio.findById(1L)).thenReturn(Optional.empty());

        PreferenciaNotificacionResponse respuesta = servicio.obtener(1L);

        assertThat(respuesta.campanas()).isTrue();
        assertThat(respuesta.suscripciones()).isTrue();
        assertThat(respuesta.novedades()).isTrue();
        assertThat(respuesta.todasActivas()).isTrue();
        verify(repositorio, never()).save(any());
    }

    @Test
    void cambiaSoloElInterruptorIndicado() {
        PreferenciaNotificacion guardada = new PreferenciaNotificacion(1L);
        when(repositorio.findById(1L)).thenReturn(Optional.of(guardada));
        when(repositorio.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        PreferenciaNotificacionResponse respuesta = servicio.actualizar(
                1L, new PreferenciaNotificacionRequest(false, null, null));

        assertThat(respuesta.campanas()).isFalse();
        assertThat(respuesta.suscripciones()).isTrue();
        assertThat(respuesta.novedades()).isTrue();
        assertThat(respuesta.todasActivas()).isFalse();
        assertThat(guardada.isCampanas()).isFalse();
    }

    @Test
    void unAvisoApagadoNoSeEnvia() {
        PreferenciaNotificacion guardada = new PreferenciaNotificacion(1L);
        guardada.setSuscripciones(false);
        when(repositorio.findById(1L)).thenReturn(Optional.of(guardada));

        assertThat(servicio.permite(1L, TipoNotificacion.SUSCRIPCION)).isFalse();
        assertThat(servicio.permite(1L, TipoNotificacion.CAMPANA)).isTrue();
        assertThat(servicio.permite(1L, TipoNotificacion.SISTEMA)).isTrue();
    }

    @Test
    void sinFilaGuardadaLosAvisosSePermiten() {
        when(repositorio.findById(1L)).thenReturn(Optional.empty());

        assertThat(servicio.permite(1L, TipoNotificacion.CAMPANA)).isTrue();
        assertThat(servicio.permite(1L, TipoNotificacion.SUSCRIPCION)).isTrue();
    }
}
