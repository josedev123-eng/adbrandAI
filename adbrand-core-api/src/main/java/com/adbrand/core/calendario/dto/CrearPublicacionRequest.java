package com.adbrand.core.calendario.dto;

import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

// HU 33: datos para programar una publicación desde el calendario.
public record CrearPublicacionRequest(
        @NotBlank(message = "Escribe un título para la publicación.")
        @Size(max = 150, message = "El título tiene como máximo 150 caracteres.")
        String titulo,

        @NotNull(message = "Selecciona la red social.")
        RedSocialPublicacion redSocial,

        @NotNull(message = "Selecciona la fecha.")
        LocalDate fecha,

        @NotNull(message = "Selecciona la hora.")
        LocalTime hora) {
}
