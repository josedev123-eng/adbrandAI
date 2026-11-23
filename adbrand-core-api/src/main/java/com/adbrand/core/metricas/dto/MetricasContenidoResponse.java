package com.adbrand.core.metricas.dto;

import java.util.List;

// HU 57: las métricas del contenido que muestra la app móvil.
// Los tres estados siempre vienen, aunque estén en cero (criterio 2: sin contenido, todo en cero).
public record MetricasContenidoResponse(
        long total,
        long aprobados,
        long enRevision,
        long rechazados,
        List<ContenidoPorMes> porMes) {

    // Cuántos anuncios se generaron en un mes, por ejemplo {"mes": "2026-11", "cantidad": 3}.
    public record ContenidoPorMes(String mes, long cantidad) {
    }
}
