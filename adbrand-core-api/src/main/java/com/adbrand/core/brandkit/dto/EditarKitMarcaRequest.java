package com.adbrand.core.brandkit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

// Lo que envía la web al editar el kit de marca (HU 32).
public record EditarKitMarcaRequest(
        @NotEmpty(message = "Agrega al menos un color.")
        @Size(max = 8, message = "Máximo 8 colores.")
        List<@Valid ColorKit> colores,

        @NotBlank(message = "Ingresa la tipografía principal.")
        @Size(max = 200, message = "Máximo 200 caracteres.")
        String tipografiaPrimaria,

        @Size(max = 200, message = "Máximo 200 caracteres.")
        String tipografiaSecundaria,

        @NotBlank(message = "Describe el concepto del logotipo.")
        @Size(max = 2000, message = "Máximo 2000 caracteres.")
        String logoConcepto) {

    public EditarKitMarcaRequest {
        tipografiaPrimaria = tipografiaPrimaria == null ? null : tipografiaPrimaria.trim();
        tipografiaSecundaria = tipografiaSecundaria == null ? null : tipografiaSecundaria.trim();
        logoConcepto = logoConcepto == null ? null : logoConcepto.trim();
    }

    // Un color de la paleta: el código #RRGGBB y, si se quiere, su nombre o uso.
    public record ColorKit(
            @NotBlank(message = "Escribe el color.")
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Escribe el color como #RRGGBB.")
            String hex,

            @Size(max = 100, message = "Máximo 100 caracteres.")
            String nombre) {
    }
}
