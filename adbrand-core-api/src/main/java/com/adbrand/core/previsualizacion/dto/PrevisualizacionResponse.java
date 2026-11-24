package com.adbrand.core.previsualizacion.dto;

import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import java.util.List;

// HU 45: { kit: {...} o null, anuncios: [...] }. Si el negocio no tiene kit, la app muestra un aviso.
// HU 46: formato elegido (cuadrado, vertical u horizontal) con su tamaño y cuánto texto entra.
public record PrevisualizacionResponse(
        FormatoResponse formato,
        KitMarcaGeneradoResponse kit,
        List<AnuncioPrevisualizacion> anuncios) {

    public PrevisualizacionResponse(KitMarcaGeneradoResponse kit, List<AnuncioPrevisualizacion> anuncios) {
        this(FormatoResponse.desde(FormatoAnuncio.CUADRADO), kit, anuncios);
    }

    public record FormatoResponse(String codigo, String nombre, String uso, int ancho, int alto, int maxCaracteres) {
        public static FormatoResponse desde(FormatoAnuncio formato) {
            return new FormatoResponse(formato.name(), formato.nombre(), formato.uso(), formato.ancho(), formato.alto(),
                    formato.maxCaracteres());
        }
    }
}
