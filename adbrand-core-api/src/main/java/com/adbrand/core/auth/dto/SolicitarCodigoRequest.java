package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Lo que envía la web para pedir un código de recuperación (HU 24).
public record SolicitarCodigoRequest(
        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.")
        String correo) {

    public SolicitarCodigoRequest {
        correo = correo == null ? null : correo.trim();
    }
}
