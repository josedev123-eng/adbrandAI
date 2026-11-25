package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.DecisionDueno;
import com.adbrand.core.contenido.entity.EstadoContenido;
import java.time.LocalDateTime;

// HU 56: un contenido con el estado que ve el dueño en el móvil, en una sola palabra:
//   PENDIENTE           todavía no lo aprueba ni lo rechaza (está en "por revisar").
//   APROBADO            el dueño lo aprobó (HU 53).
//   RECHAZADO           el dueño lo rechazó (HU 54); motivo es lo que escribió.
//   EN_REVISION         un moderador todavía lo está revisando.
//   RECHAZADO_MODERADOR un moderador lo rechazó; motivo es lo que escribió el moderador.
public record EstadoActualResponse(
        Long id,
        String oferta,
        String redSocial,
        String estadoActual,
        String motivo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        // El titular del anuncio (la primera frase de la IA), para mostrarlo en vez del pedido del dueño.
        String titular,
        // El texto del anuncio, para buscar en el celular por lo que dice.
        String texto) {

    public static EstadoActualResponse desde(Contenido contenido) {
        String estadoActual;
        String motivo = null;

        // Lo que decidió el dueño manda; si no decidió, se muestra lo del filtro o el moderador.
        if (contenido.getDecisionDueno() == DecisionDueno.RECHAZADO) {
            estadoActual = "RECHAZADO";
            motivo = contenido.getMotivoDueno();
        } else if (contenido.getEstado() == EstadoContenido.RECHAZADO) {
            estadoActual = "RECHAZADO_MODERADOR";
            motivo = contenido.getMotivoRevision();
        } else if (contenido.getEstado() == EstadoContenido.DUDOSO) {
            estadoActual = "EN_REVISION";
        } else if (contenido.getDecisionDueno() == DecisionDueno.APROBADO) {
            estadoActual = "APROBADO";
        } else {
            estadoActual = "PENDIENTE";
        }

        return new EstadoActualResponse(
                contenido.getId(),
                contenido.getOferta(),
                contenido.getRedSocial().name(),
                estadoActual,
                motivo,
                contenido.getFechaCreacion(),
                contenido.getFechaActualizacion(),
                com.adbrand.core.contenido.service.VideoAnuncioService.titularDe(contenido),
                contenido.getTexto());
    }
}
