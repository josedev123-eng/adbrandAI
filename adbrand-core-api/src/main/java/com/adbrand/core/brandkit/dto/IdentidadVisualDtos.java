package com.adbrand.core.brandkit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

// Identidad de marca (V70): logo como imagen y eslogan elegido.
public final class IdentidadVisualDtos {

    private IdentidadVisualDtos() {
    }

    // La imagen va como data URL ("data:image/png;base64,..."). origen: SUBIDO (ya tenía logo) o IA (lo eligió).
    public record GuardarLogoRequest(
            @NotBlank(message = "Elige o sube una imagen.")
            String imagen,

            @NotBlank(message = "Indica de dónde viene el logo.")
            @Pattern(regexp = "SUBIDO|IA", message = "El origen debe ser SUBIDO o IA.")
            String origen) {
    }

    public record ElegirEsloganRequest(
            @NotBlank(message = "Escribe o elige un eslogan.")
            @Size(max = 150, message = "El eslogan puede tener hasta 150 caracteres.")
            String eslogan) {
    }

    // Las propuestas de logo de la IA, como data URL, para que la empresa elija una. No se guardan hasta elegir.
    public record OpcionesLogoResponse(List<String> opciones, boolean simulado) {
    }

    // Editar el logo con una instrucción corta ("pon la taza más grande"), como los retoques de anuncios.
    public record RetocarLogoRequest(
            @NotBlank(message = "Escribe qué quieres cambiar del logo.")
            @Size(max = 300, message = "Máximo 300 caracteres.")
            String instruccion) {
    }

    // La versión nueva (data URL). No se guarda hasta que la empresa diga "Usar esta versión" (PUT /logo-imagen).
    // retoquesRestantes: null en modo de prueba.
    public record RetoqueLogoResponse(String imagen, boolean simulado, Integer retoquesRestantes) {
    }
}
