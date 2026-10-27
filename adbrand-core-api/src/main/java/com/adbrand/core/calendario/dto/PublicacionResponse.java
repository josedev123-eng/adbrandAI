package com.adbrand.core.calendario.dto;

import com.adbrand.core.calendario.entity.PublicacionProgramada;
import java.time.LocalDate;
import java.time.LocalTime;

// Lo que dibuja el calendario mensual (HU 33).
public record PublicacionResponse(
        Long id,
        String titulo,
        String redSocial,
        LocalDate fecha,
        LocalTime hora,
        String estado) {

    public static PublicacionResponse desde(PublicacionProgramada publicacion) {
        return new PublicacionResponse(
                publicacion.getId(),
                publicacion.getTitulo(),
                publicacion.getRedSocial().name(),
                publicacion.getFecha(),
                publicacion.getHora(),
                publicacion.getEstado().name());
    }
}
