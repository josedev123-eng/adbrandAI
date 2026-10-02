package com.adbrand.core.contenido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Lo que envía la web para pedir un anuncio.
public record GenerarAnuncioRequest(
        @NotBlank(message = "Escribe la oferta que quieres anunciar.")
        @Size(max = 300, message = "Máximo 300 caracteres.")
        String oferta,

        @NotNull(message = "Selecciona la red social.")
        RedSocial redSocial) {
}