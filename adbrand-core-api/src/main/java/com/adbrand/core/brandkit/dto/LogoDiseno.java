package com.adbrand.core.brandkit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// HU 29: el logo que el dueño diseña en el editor. Son solo datos; la web dibuja el logo con ellos.
public record LogoDiseno(
        @NotBlank(message = "Escribe el texto o las iniciales del logo.")
        @Size(max = 20, message = "Máximo 20 caracteres.")
        String texto,

        @NotNull(message = "Elige la forma del logo.")
        FormaLogo forma,

        @NotBlank(message = "Elige el color de fondo.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Escribe el color como #RRGGBB.")
        String colorFondo,

        @NotBlank(message = "Elige el color del texto.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Escribe el color como #RRGGBB.")
        String colorTexto,

        @NotBlank(message = "Elige la tipografía del logo.")
        @Size(max = 60, message = "Máximo 60 caracteres.")
        String tipografia) {

    public LogoDiseno {
        texto = texto == null ? null : texto.trim();
        colorFondo = colorFondo == null ? null : colorFondo.toUpperCase(java.util.Locale.ROOT);
        colorTexto = colorTexto == null ? null : colorTexto.toUpperCase(java.util.Locale.ROOT);
        tipografia = tipografia == null ? null : tipografia.trim();
    }

    public enum FormaLogo {
        CIRCULO,
        CUADRADO,
        REDONDEADO
    }
}
