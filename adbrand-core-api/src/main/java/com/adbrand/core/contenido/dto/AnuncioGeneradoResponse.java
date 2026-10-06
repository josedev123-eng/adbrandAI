package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.negocio.entity.Tono;

// El anuncio generado. "simulado" avisa si vino del modo de prueba;
// "estado" y "motivoRevision" dicen si pasó el filtro automático (HU 13).
public record AnuncioGeneradoResponse(
        Long id,
        String texto,
        RedSocial redSocial,
        Tono tono,
        String oferta,
        boolean simulado,
        EstadoContenido estado,
        String motivoRevision) {

    public static AnuncioGeneradoResponse desde(Contenido contenido, boolean simulado) {
        return new AnuncioGeneradoResponse(
                contenido.getId(),
                contenido.getTexto(),
                contenido.getRedSocial(),
                contenido.getTono(),
                contenido.getOferta(),
                simulado,
                contenido.getEstado(),
                contenido.getMotivoRevision());
    }
}