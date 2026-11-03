package com.adbrand.core.brandkit.dto;

import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import java.time.LocalDateTime;

// Una edición del kit en el historial (HU 32).
public record KitMarcaVersionResponse(
        Long id,
        String paletaColores,
        String tipografiaPrimaria,
        String tipografiaSecundaria,
        String logoConcepto,
        LocalDateTime fechaCreacion) {

    public static KitMarcaVersionResponse desde(KitMarcaVersion version) {
        return new KitMarcaVersionResponse(version.getId(), version.getPaletaColores(),
                version.getTipografiaPrimaria(), version.getTipografiaSecundaria(),
                version.getLogoConcepto(), version.getFechaCreacion());
    }
}
