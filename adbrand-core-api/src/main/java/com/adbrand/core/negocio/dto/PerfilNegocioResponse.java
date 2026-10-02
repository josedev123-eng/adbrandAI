package com.adbrand.core.negocio.dto;

import com.adbrand.core.negocio.entity.PerfilNegocio;
import com.adbrand.core.negocio.entity.Tono;
import java.time.LocalDateTime;

// Lo que la API devuelve a la web.
public record PerfilNegocioResponse(
        Long id,
        String nombreComercial,
        String rubro,
        String publicoObjetivo,
        Tono tono,
        String descripcion,
        LocalDateTime fechaActualizacion) {

    public static PerfilNegocioResponse desde(PerfilNegocio perfil) {
        return new PerfilNegocioResponse(
                perfil.getId(),
                perfil.getNombreComercial(),
                perfil.getRubro(),
                perfil.getPublicoObjetivo(),
                perfil.getTono(),
                perfil.getDescripcion(),
                perfil.getFechaActualizacion());
    }
}