package com.adbrand.core.auth.firebase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos que manda la app móvil para entrar o registrarse con Firebase.
public final class FirebaseDtos {

    private FirebaseDtos() {
    }

    // Iniciar sesión o vincular: solo el ID token que dio Firebase.
    public record TokenFirebaseRequest(
            @NotBlank(message = "Falta el token de Firebase.")
            String idToken) {
    }

    // Registro desde la app: el correo sale del token; el nombre y la empresa los escribe el usuario.
    public record RegistroFirebaseRequest(
            @NotBlank(message = "Falta el token de Firebase.")
            String idToken,

            @NotBlank(message = "Ingresa tu nombre.")
            @Size(max = 100, message = "Máximo 100 caracteres.")
            String nombre,

            @NotBlank(message = "Ingresa el nombre de tu empresa.")
            @Size(max = 150, message = "Máximo 150 caracteres.")
            String nombreEmpresa) {

        public RegistroFirebaseRequest {
            nombre = nombre == null ? null : nombre.trim();
            nombreEmpresa = nombreEmpresa == null ? null : nombreEmpresa.trim();
        }
    }
}
