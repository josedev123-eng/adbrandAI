package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import java.time.LocalDateTime;

// HU 53 y 54: un contenido en la lista "por revisar" del móvil.
// puedeAprobar es false si el moderador lo rechazó o si todavía está en su bandeja (DUDOSO).
// motivoDueno: lo que escribió el dueño al rechazarlo (HU 54).
public record ContenidoPorRevisarResponse(
        Long id,
        String oferta,
        String texto,
        String redSocial,
        String tono,
        String estado,
        String motivoRevision,
        String decisionDueno,
        String motivoDueno,
        boolean puedeAprobar,
        LocalDateTime fechaCreacion) {

    public static ContenidoPorRevisarResponse desde(Contenido contenido) {
        return new ContenidoPorRevisarResponse(
                contenido.getId(),
                contenido.getOferta(),
                contenido.getTexto(),
                contenido.getRedSocial().name(),
                contenido.getTono().name(),
                contenido.getEstado().name(),
                contenido.getMotivoRevision(),
                contenido.getDecisionDueno().name(),
                contenido.getMotivoDueno(),
                contenido.getEstado() == EstadoContenido.APROBADO,
                contenido.getFechaCreacion());
    }
}
