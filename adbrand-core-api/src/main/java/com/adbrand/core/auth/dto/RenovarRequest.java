package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

// HU 43: la app envía el token de renovación que guardó al iniciar sesión.
public record RenovarRequest(
        @NotBlank(message = "Envía el token de renovación.")
        String tokenRenovacion) {
}
