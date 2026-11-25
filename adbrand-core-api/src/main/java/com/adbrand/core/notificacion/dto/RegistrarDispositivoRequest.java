package com.adbrand.core.notificacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// HU 50: lo que envía la app al iniciar sesión o cuando Firebase renueva el token.
public record RegistrarDispositivoRequest(
        @NotBlank(message = "El token del dispositivo es obligatorio.")
        @Size(max = 255, message = "El token no puede superar los 255 caracteres.")
        String fcmToken,

        @Pattern(regexp = "(?i)android|ios", message = "La plataforma debe ser android o ios.")
        String platform,

        @Size(max = 120, message = "El nombre del equipo no puede superar los 120 caracteres.")
        String deviceName) {
}
