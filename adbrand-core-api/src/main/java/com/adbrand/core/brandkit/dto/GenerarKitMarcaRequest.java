package com.adbrand.core.brandkit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Lo que envía la web para pedir el Kit de Marca.
public record GenerarKitMarcaRequest(
        @NotBlank(message = "El nombre del negocio es obligatorio.")
        @Size(max = 120, message = "Máximo 120 caracteres.")
        String nombreNegocio,

        @NotBlank(message = "El sector/rubro es obligatorio.")
        @Size(max = 80, message = "Máximo 80 caracteres.")
        String sector,

        @Size(max = 300, message = "Máximo 300 caracteres.")
        String paletaSugerida,

        @Size(max = 500, message = "Máximo 500 caracteres.")
        String valoresEslogan,

        @NotBlank(message = "El estilo visual es obligatorio.")
        @Size(max = 100, message = "Máximo 100 caracteres.")
        String estiloVisual) {
}