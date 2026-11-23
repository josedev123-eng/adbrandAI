package com.adbrand.core.brandkit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo que envía la web (o la app) para pedir el Kit de Marca.
// Identidad de marca (V70): sentimiento, diferencial y eslogan son opcionales. Si la empresa ya tiene eslogan
// se usa tal cual; si no, la IA propone 3.
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
        String estiloVisual,

        @Size(max = 100, message = "Máximo 100 caracteres.")
        String sentimiento,

        @Size(max = 300, message = "Máximo 300 caracteres.")
        String diferencial,

        @Size(max = 150, message = "El eslogan puede tener hasta 150 caracteres.")
        String eslogan) {

    // Forma de antes (sin identidad), para quien todavía la usa.
    public GenerarKitMarcaRequest(String nombreNegocio, String sector, String paletaSugerida,
                                  String valoresEslogan, String estiloVisual) {
        this(nombreNegocio, sector, paletaSugerida, valoresEslogan, estiloVisual, null, null, null);
    }

    public boolean tieneEslogan() {
        return eslogan != null && !eslogan.isBlank();
    }
}
