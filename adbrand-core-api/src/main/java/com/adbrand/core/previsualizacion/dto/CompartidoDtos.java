package com.adbrand.core.previsualizacion.dto;

import java.time.OffsetDateTime;

// HU 47: datos del registro de un anuncio compartido.
public final class CompartidoDtos {

    private CompartidoDtos() {
    }

    // Formato en que se compartió (cuadrado si no se manda).
    public record CompartidoRequest(String formato) {
    }

    // El registro guardado y cuántas veces se compartió ese anuncio en total.
    public record CompartidoResponse(Long contenidoId, String formato, OffsetDateTime fecha, long vecesCompartido) {
    }
}
