package com.adbrand.core.calendario.dto;

import com.adbrand.core.calendario.entity.Campana;
import java.time.LocalDate;

// HU 35: una campaña como la ve la web (lista de campañas y filtro del calendario).
public record CampanaResponse(
        Long id,
        String nombre,
        String color,
        String tipo,
        LocalDate fechaInicio,
        LocalDate fechaFin) {

    public static CampanaResponse desde(Campana campana) {
        return new CampanaResponse(
                campana.getId(),
                campana.getNombre(),
                campana.getColor(),
                campana.getTipo().name(),
                campana.getFechaInicio(),
                campana.getFechaFin());
    }
}
