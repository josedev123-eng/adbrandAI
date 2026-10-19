package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Lo que envía la web para cambiar la contraseña con el código recibido (HU 24).
public record CambiarContrasenaRequest(
        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.")
        String correo,

        @NotBlank(message = "Ingresa el código que te enviamos.")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos.")
        String codigo,

        @NotBlank(message = "Ingresa una contraseña nueva.")
        @Size(min = ReglasContrasena.MINIMO, max = ReglasContrasena.MAXIMO, message = ReglasContrasena.MENSAJE_LARGO)
        @Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE_PATRON)
        String contrasenaNueva) {

    public CambiarContrasenaRequest {
        correo = correo == null ? null : correo.trim();
        codigo = codigo == null ? null : codigo.trim();
    }
}
