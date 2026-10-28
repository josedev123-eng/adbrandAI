package com.adbrand.core.reporte.dto;

import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import java.time.LocalDateTime;

// HU 37: una fila del informe consolidado. Cada anuncio generado es una campaña de la empresa.
public record CampanaInformeResponse(
        Long id,
        LocalDateTime fecha,
        RedSocial redSocial,
        String oferta,
        EstadoContenido estado) {

    public static CampanaInformeResponse desde(Contenido contenido) {
        return new CampanaInformeResponse(contenido.getId(), contenido.getFechaCreacion(), contenido.getRedSocial(),
                contenido.getOferta(), contenido.getEstado());
    }
}
