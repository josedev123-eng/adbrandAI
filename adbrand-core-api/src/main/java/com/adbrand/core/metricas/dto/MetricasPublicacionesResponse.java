package com.adbrand.core.metricas.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// HU 58: las métricas de las publicaciones del calendario que muestra la app móvil.
// Las dos redes siempre vienen, aunque estén en cero.
public record MetricasPublicacionesResponse(
        long programadas,
        long publicadas,
        long canceladas,
        List<PublicacionesPorRed> porRed,
        List<ProximaPublicacion> proximas) {

    // Programadas y publicadas de una red social, por ejemplo INSTAGRAM.
    public record PublicacionesPorRed(String redSocial, long programadas, long publicadas) {
    }

    // Una de las siguientes publicaciones programadas, con su campaña si tiene.
    public record ProximaPublicacion(
            Long id,
            String titulo,
            String redSocial,
            LocalDate fecha,
            LocalTime hora,
            String campanaNombre,
            String campanaColor) {
    }
}
