package com.adbrand.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Lo que envía la web al registrar una empresa (HU 21).
public record RegistroRequest(
        @NotBlank(message = "Ingresa tu nombre.")
        @Size(max = 100, message = "Máximo 100 caracteres.")
        String nombre,

        @NotBlank(message = "Ingresa el nombre de tu empresa.")
        @Size(max = 150, message = "Máximo 150 caracteres.")
        String nombreEmpresa,

        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.")
        @Size(max = 150, message = "Máximo 150 caracteres.")
        String correo,

        @NotBlank(message = "Ingresa una contraseña.")
        @Size(min = ReglasContrasena.MINIMO, max = ReglasContrasena.MAXIMO, message = ReglasContrasena.MENSAJE_LARGO)
        @Pattern(regexp = ReglasContrasena.PATRON, message = ReglasContrasena.MENSAJE_PATRON)
        String contrasena) {

    // Quita los espacios de los extremos antes de validar, así " ana@mail.com " cuenta como correo válido.
    // La contraseña no se toca: un espacio en ella es un error que se debe mostrar.
    public RegistroRequest {
        nombre = nombre == null ? null : nombre.trim();
        nombreEmpresa = nombreEmpresa == null ? null : nombreEmpresa.trim();
        correo = correo == null ? null : correo.trim();
    }
}
