package com.adbrand.core.pago.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// HU 27: datos que manda la web para generar el QR de Yape. El celular es opcional,
// pero si viene debe tener los 9 dígitos de un número peruano.
public record RegistrarYapeRequest(
        @NotNull(message = "Selecciona un plan.")
        Long planId,

        @Pattern(regexp = "9[0-9]{8}", message = "El celular debe tener 9 dígitos y empezar con 9.")
        String celular) {
}
