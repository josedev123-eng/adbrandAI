package com.adbrand.core.imagen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo que envía la web para pedir una imagen: qué debe mostrar.
public record PedirImagenRequest(
        @NotBlank(message = "Describe la imagen que quieres.")
        @Size(max = 500, message = "Máximo 500 caracteres.")
        String descripcion) {

    public PedirImagenRequest {
        descripcion = descripcion == null ? null : descripcion.trim();
    }
}
