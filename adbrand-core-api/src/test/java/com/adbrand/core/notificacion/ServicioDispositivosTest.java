package com.adbrand.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.notificacion.dto.DispositivoResponse;
import com.adbrand.core.notificacion.dto.RegistrarDispositivoRequest;
import com.adbrand.core.notificacion.entity.DispositivoUsuario;
import com.adbrand.core.notificacion.entity.Plataforma;
import com.adbrand.core.notificacion.repository.DispositivoUsuarioRepository;
import com.adbrand.core.notificacion.service.ServicioDispositivos;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// HU 50: el token se guarda una sola vez y se reasigna si cambia de cuenta.
@ExtendWith(MockitoExtension.class)
class ServicioDispositivosTest {

    @Mock
    private DispositivoUsuarioRepository repositorio;

    @InjectMocks
    private ServicioDispositivos servicio;

    private final RegistrarDispositivoRequest datos =
            new RegistrarDispositivoRequest(" token-abc ", "android", "Pixel 6");

    @Test
    void registraUnTokenNuevoYLoDejaActivo() {
        when(repositorio.findByFcmToken("token-abc")).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        DispositivoResponse respuesta = servicio.registrar(7L, datos);

        assertThat(respuesta.platform()).isEqualTo("android");
        assertThat(respuesta.deviceName()).isEqualTo("Pixel 6");
        assertThat(respuesta.activo()).isTrue();
        verify(repositorio).saveAndFlush(any());
    }

    @Test
    void reasignaElTokenSiElEquipoCambiaDeCuenta() {
        DispositivoUsuario existente = new DispositivoUsuario();
        existente.setUsuarioId(1L);
        existente.setFcmToken("token-abc");
        when(repositorio.findByFcmToken("token-abc")).thenReturn(Optional.of(existente));
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.registrar(9L, datos);

        assertThat(existente.getUsuarioId()).isEqualTo(9L);
        assertThat(existente.isActivo()).isTrue();
        assertThat(existente.getPlatform()).isEqualTo(Plataforma.ANDROID);
    }

    @Test
    void desactivaElTokenDelUsuario() {
        DispositivoUsuario existente = new DispositivoUsuario();
        existente.setUsuarioId(9L);
        existente.setFcmToken("token-abc");
        when(repositorio.findByFcmToken("token-abc")).thenReturn(Optional.of(existente));

        servicio.desactivar(9L, "token-abc");

        assertThat(existente.isActivo()).isFalse();
        verify(repositorio).save(existente);
    }

    @Test
    void noTocaElTokenDeOtraCuenta() {
        DispositivoUsuario existente = new DispositivoUsuario();
        existente.setUsuarioId(1L);
        existente.setFcmToken("token-abc");
        when(repositorio.findByFcmToken("token-abc")).thenReturn(Optional.of(existente));

        servicio.desactivar(9L, "token-abc");

        assertThat(existente.isActivo()).isTrue();
    }
}
