package com.adbrand.core.brandkit.dto;

import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import java.time.LocalDateTime;

// Una edición del kit en el historial (HU 32). logoDiseno es el logo del editor (HU 29).
public record KitMarcaVersionResponse(
        Long id,
        String paletaColores,
        String tipografiaPrimaria,
        String tipografiaSecundaria,
        String logoConcepto,
        LogoDiseno logoDiseno,
        LocalDateTime fechaCreacion) {

    public static KitMarcaVersionResponse desde(KitMarcaVersion version) {
        return new KitMarcaVersionResponse(version.getId(), version.getPaletaColores(),
                version.getTipografiaPrimaria(), version.getTipografiaSecundaria(),
                version.getLogoConcepto(), LogoDisenoJson.desdeTexto(version.getLogoDiseno()),
                version.getFechaCreacion());
    }
}
