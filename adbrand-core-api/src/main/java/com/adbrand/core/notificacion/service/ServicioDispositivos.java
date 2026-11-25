package com.adbrand.core.notificacion.service;

import com.adbrand.core.notificacion.dto.DispositivoResponse;
import com.adbrand.core.notificacion.dto.RegistrarDispositivoRequest;
import com.adbrand.core.notificacion.entity.DispositivoUsuario;
import com.adbrand.core.notificacion.entity.Plataforma;
import com.adbrand.core.notificacion.repository.DispositivoUsuarioRepository;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 50: registro y baja de los teléfonos que reciben los avisos.
@Service
public class ServicioDispositivos {

    private final DispositivoUsuarioRepository repositorio;

    public ServicioDispositivos(DispositivoUsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra o actualiza el token. Si el mismo equipo inicia sesión con otra
     * cuenta, el token se reasigna: nunca queda duplicado.
     */
    @Transactional
    public DispositivoResponse registrar(Long usuarioId, RegistrarDispositivoRequest datos) {
        String token = datos.fcmToken() == null ? "" : datos.fcmToken().trim();
        if (token.isEmpty()) {
            throw new SolicitudInvalidaException("TOKEN_REQUERIDO", "El token del dispositivo es obligatorio.");
        }

        DispositivoUsuario dispositivo = repositorio.findByFcmToken(token).orElseGet(DispositivoUsuario::new);
        dispositivo.setFcmToken(token);
        dispositivo.setUsuarioId(usuarioId);
        dispositivo.setPlatform(Plataforma.desde(datos.platform()));
        dispositivo.setDeviceName(datos.deviceName());
        dispositivo.setActivo(true);
        dispositivo.setLastUsedAt(LocalDateTime.now());
        repositorio.saveAndFlush(dispositivo);
        return DispositivoResponse.desde(dispositivo);
    }

    // Baja idempotente: si el token ya no existe, la app no recibe un error.
    @Transactional
    public void desactivar(Long usuarioId, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        repositorio.findByFcmToken(token.trim())
                .filter(dispositivo -> dispositivo.getUsuarioId().equals(usuarioId))
                .ifPresent(dispositivo -> {
                    dispositivo.setActivo(false);
                    repositorio.save(dispositivo);
                });
    }
}
