package com.adbrand.core.previsualizacion.dto;

import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import java.util.List;

// HU 45: { kit: {...} o null, anuncios: [...] }. Si el negocio no tiene kit, la app muestra un aviso.
public record PrevisualizacionResponse(
        KitMarcaGeneradoResponse kit,
        List<AnuncioPrevisualizacion> anuncios) {
}
