package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Lo que envía la web para confirmar el correo con el código de 6 dígitos que le llegó.
public record VerificarCorreoRequest(
        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.")
        String correo,

        @NotBlank(message = "Ingresa el código que te enviamos.")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos.")
        String codigo) {

    public VerificarCorreoRequest {
        correo = correo == null ? null : correo.trim();
        codigo = codigo == null ? null : codigo.trim();
    }
}
