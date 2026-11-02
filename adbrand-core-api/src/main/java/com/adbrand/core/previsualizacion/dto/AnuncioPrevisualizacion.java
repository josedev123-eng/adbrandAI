package com.adbrand.core.previsualizacion.dto;

import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.negocio.entity.Tono;
import java.time.LocalDateTime;

// HU 45: un anuncio listo para dibujarse en la previsualización de la app.
public record AnuncioPrevisualizacion(
        Long id,
        String texto,
        RedSocial redSocial,
        Tono tono,
        EstadoContenido estado,
        LocalDateTime fechaCreacion) {

    public static AnuncioPrevisualizacion desde(Contenido contenido) {
        return new AnuncioPrevisualizacion(
                contenido.getId(),
                contenido.getTexto(),
                contenido.getRedSocial(),
                contenido.getTono(),
                contenido.getEstado(),
                contenido.getFechaCreacion());
    }
}
