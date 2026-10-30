package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

// HU 43: al cerrar sesión se revoca el token de renovación.
public record CerrarSesionRequest(
        @NotBlank(message = "Envía el token de renovación.")
        String tokenRenovacion) {
}
