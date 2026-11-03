package com.adbrand.core.calendario.dto;

import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import java.time.LocalDate;
import java.time.LocalTime;

// Lo que dibuja el calendario mensual (HU 33).
// HU 35: si la publicación es de una campaña, también lleva su id, nombre y color; si no, esos tres van en null.
public record PublicacionResponse(
        Long id,
        String titulo,
        String redSocial,
        LocalDate fecha,
        LocalTime hora,
        String estado,
        Long campanaId,
        String campanaNombre,
        String campanaColor) {

    // Publicación sin campaña (la usan las pruebas de la HU 33).
    public PublicacionResponse(Long id, String titulo, String redSocial, LocalDate fecha, LocalTime hora, String estado) {
        this(id, titulo, redSocial, fecha, hora, estado, null, null, null);
    }

    public static PublicacionResponse desde(PublicacionProgramada publicacion) {
        return desde(publicacion, null);
    }

    public static PublicacionResponse desde(PublicacionProgramada publicacion, Campana campana) {
        return new PublicacionResponse(
                publicacion.getId(),
                publicacion.getTitulo(),
                publicacion.getRedSocial().name(),
                publicacion.getFecha(),
                publicacion.getHora(),
                publicacion.getEstado().name(),
                campana == null ? null : campana.getId(),
                campana == null ? null : campana.getNombre(),
                campana == null ? null : campana.getColor());
    }
}
