package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Mi cuenta: la empresa conectada cambia su contraseña escribiendo la actual.
public record CambiarMiContrasenaRequest(
        @NotBlank(message = "Escribe tu contraseña actual.")
        String actual,

        @NotBlank(message = "Escribe la contraseña nueva.")
        @Size(min = ReglasContrasena.MINIMO, max = ReglasContrasena.MAXIMO, message = ReglasContrasena.MENSAJE_LARGO)
        @Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE_PATRON)
        String nueva) {
}
