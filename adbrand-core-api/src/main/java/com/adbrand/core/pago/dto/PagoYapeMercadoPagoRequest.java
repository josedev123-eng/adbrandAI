package com.adbrand.core.pago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// HU 27 con Mercado Pago: la web ya convirtió el celular y el código de aprobación de Yape en un token.
// El código de aprobación nunca llega a nuestro servidor.
public record PagoYapeMercadoPagoRequest(
        @NotNull(message = "Selecciona un plan.")
        Long planId,

        @NotBlank(message = "Falta el token de Yape.")
        @Size(max = 100, message = "El token de Yape no es válido.")
        String token,

        @NotBlank(message = "Escribe tu número de celular.")
        @Pattern(regexp = "9[0-9]{8}|1[0-9]{8}", message = "El celular tiene 9 dígitos.")
        String celular,

        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.")
        String correo) {
}
