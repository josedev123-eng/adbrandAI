package com.adbrand.core.previsualizacion.dto;

import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.negocio.entity.Tono;
import java.time.LocalDateTime;

// HU 45: un anuncio listo para dibujarse en la previsualización de la app.
// HU 46: textoAjustado es el texto que entra en el formato elegido; recortado = se acortó con "…".
public record AnuncioPrevisualizacion(
        Long id,
        String texto,
        String textoAjustado,
        boolean recortado,
        RedSocial redSocial,
        Tono tono,
        EstadoContenido estado,
        LocalDateTime fechaCreacion) {

    public static AnuncioPrevisualizacion desde(Contenido contenido) {
        return desde(contenido, FormatoAnuncio.CUADRADO);
    }

    public static AnuncioPrevisualizacion desde(Contenido contenido, FormatoAnuncio formato) {
        String ajustado = formato.ajustar(contenido.getTexto());
        return new AnuncioPrevisualizacion(
                contenido.getId(),
                contenido.getTexto(),
                ajustado,
                !ajustado.equals(contenido.getTexto() == null ? "" : contenido.getTexto().strip()),
                contenido.getRedSocial(),
                contenido.getTono(),
                contenido.getEstado(),
                contenido.getFechaCreacion());
    }
}
