package com.adbrand.core.brandkit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// HU 31: lo que envía el editor de identidad visual. Solo cambia tipografías y voz de marca.
public record EditarIdentidadRequest(
        @NotBlank(message = "Elige la tipografía principal.")
        @Size(max = 200, message = "Máximo 200 caracteres.")
        String tipografiaPrimaria,

        @Size(max = 200, message = "Máximo 200 caracteres.")
        String tipografiaSecundaria,

        @NotBlank(message = "Describe la voz de tu marca.")
        @Size(max = 1000, message = "Máximo 1000 caracteres.")
        String vozMarca) {

    public EditarIdentidadRequest {
        tipografiaPrimaria = tipografiaPrimaria == null ? null : tipografiaPrimaria.trim();
        tipografiaSecundaria = tipografiaSecundaria == null || tipografiaSecundaria.isBlank()
                ? null : tipografiaSecundaria.trim();
        vozMarca = vozMarca == null ? null : vozMarca.trim();
    }
}
