package com.adbrand.core.calendario.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// HU 36: el día nuevo al que se arrastró la publicación. La hora no cambia.
public record ReprogramarRequest(
        @NotNull(message = "Selecciona la fecha.")
        LocalDate fecha) {
}
