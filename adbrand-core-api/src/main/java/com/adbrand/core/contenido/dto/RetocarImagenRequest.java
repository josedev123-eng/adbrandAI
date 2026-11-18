package com.adbrand.core.contenido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Una instrucción corta para retocar la imagen elegida, por ejemplo "pon el fondo más claro".
public record RetocarImagenRequest(
        @NotBlank(message = "Escribe qué quieres cambiar.")
        @Size(max = 300, message = "Máximo 300 caracteres.")
        String instruccion) {

    public RetocarImagenRequest {
        instruccion = instruccion == null ? null : instruccion.trim();
    }
}
