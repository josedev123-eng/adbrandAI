package com.adbrand.core.pago.dto;

import jakarta.validation.constraints.NotNull;
// HU 27: solo se recibe el plan. El número Yape de destino lo define el servidor.
public record RegistrarYapeRequest(
        @NotNull(message = "Selecciona un plan.")
        Long planId) {
}
