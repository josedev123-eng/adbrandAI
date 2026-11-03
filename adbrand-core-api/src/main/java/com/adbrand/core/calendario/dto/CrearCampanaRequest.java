package com.adbrand.core.calendario.dto;

import com.adbrand.core.calendario.entity.TipoCampana;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// HU 35: datos para crear una campaña o un evento especial.
public record CrearCampanaRequest(
        @NotBlank(message = "Escribe el nombre de la campaña.")
        @Size(max = 100, message = "El nombre tiene como máximo 100 caracteres.")
        String nombre,

        @NotBlank(message = "Elige un color.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Escribe el color como #RRGGBB.")
        String color,

        @NotNull(message = "Elige si es una campaña o un evento.")
        TipoCampana tipo,

        @NotNull(message = "Selecciona la fecha de inicio.")
        LocalDate fechaInicio,

        @NotNull(message = "Selecciona la fecha de fin.")
        LocalDate fechaFin) {

    public CrearCampanaRequest {
        nombre = nombre == null ? null : nombre.trim();
        color = color == null ? null : color.trim();
    }
}
