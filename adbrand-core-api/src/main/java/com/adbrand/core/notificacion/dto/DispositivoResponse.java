package com.adbrand.core.notificacion.dto;

import com.adbrand.core.notificacion.entity.DispositivoUsuario;
import java.time.LocalDateTime;

// HU 50: confirmación del registro del teléfono.
public record DispositivoResponse(
        Long id,
        String platform,
        String deviceName,
        boolean activo,
        LocalDateTime lastUsedAt) {

    public static DispositivoResponse desde(DispositivoUsuario dispositivo) {
        return new DispositivoResponse(
                dispositivo.getId(),
                dispositivo.getPlatform().valor(),
                dispositivo.getDeviceName(),
                dispositivo.isActivo(),
                dispositivo.getLastUsedAt());
    }
}
