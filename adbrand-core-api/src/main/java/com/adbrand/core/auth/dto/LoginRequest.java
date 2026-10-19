package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Lo que envía la web para iniciar sesión (HU 22).
public record LoginRequest(
        @NotBlank(message = "Ingresa tu correo.")
        String correo,

        @NotBlank(message = "Ingresa tu contraseña.")
        String contrasena) {

    public LoginRequest {
        correo = correo == null ? null : correo.trim();
    }
}
