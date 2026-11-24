package com.adbrand.core.contenido.dto;

import jakarta.validation.constraints.Size;

// HU 54: motivo opcional con el que el dueño rechaza un contenido desde el móvil.
public record RechazarContenidoRequest(
        @Size(max = 300, message = "El motivo puede tener como máximo 300 caracteres.")
        String motivo) {

    public RechazarContenidoRequest {
        motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
    }
}
