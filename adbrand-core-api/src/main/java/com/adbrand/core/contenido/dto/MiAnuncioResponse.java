package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import java.time.LocalDateTime;

// Un anuncio en "Mis anuncios": su estado y, si un moderador lo revisó, qué decidió y por qué.
public record MiAnuncioResponse(
        Long id,
        String oferta,
        String texto,
        String redSocial,
        String estado,
        String motivo,
        boolean moderado,
        boolean avisoPendiente,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public static MiAnuncioResponse desde(Contenido contenido) {
        // El motivo solo se muestra si sigue en revisión o si se rechazó; en uno aprobado ya no aplica.
        String motivo = contenido.getEstado() == EstadoContenido.APROBADO ? null : contenido.getMotivoRevision();
        return new MiAnuncioResponse(
                contenido.getId(),
                contenido.getOferta(),
                contenido.getTexto(),
                contenido.getRedSocial().name(),
                contenido.getEstado().name(),
                motivo,
                contenido.getModeradorId() != null,
                contenido.isAvisoPendiente(),
                contenido.getFechaCreacion(),
                contenido.getFechaActualizacion());
    }
}
