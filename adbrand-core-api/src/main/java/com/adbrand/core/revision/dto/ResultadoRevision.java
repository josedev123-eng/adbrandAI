package com.adbrand.core.revision.dto;

import java.util.List;

// Lo que responde el filtro: si el texto pasa y, si no, por qué.
public record ResultadoRevision(boolean aprobado, List<String> motivos) {

    public static ResultadoRevision sinObservaciones() {
        return new ResultadoRevision(true, List.of());
    }

    // Une los motivos en una sola línea para guardarla en contenido.motivo_revision.
    public String motivoTexto() {
        return aprobado ? null : String.join(" ", motivos);
    }
}