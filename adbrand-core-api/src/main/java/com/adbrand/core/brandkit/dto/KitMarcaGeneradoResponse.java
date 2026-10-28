package com.adbrand.core.brandkit.dto;

// La respuesta con el Kit de Marca generado. logoDiseno es el logo del editor (HU 29), o null si aún no hay.
public record KitMarcaGeneradoResponse(
        Long id,
        String nombreNegocio,
        String sector,
        String logoConcepto,
        String tipografiaPrimaria,
        String tipografiaSecundaria,
        String paletaColores,
        String vozMarca,
        LogoDiseno logoDiseno,
        boolean simulado) {

    public static KitMarcaGeneradoResponse desde(com.adbrand.core.brandkit.entity.KitMarca kit, boolean simulado) {
        return new KitMarcaGeneradoResponse(
                kit.getId(),
                kit.getNombreNegocio(),
                kit.getSector(),
                kit.getLogoConcepto(),
                kit.getTipografiaPrimaria(),
                kit.getTipografiaSecundaria(),
                kit.getPaletaColores(),
                kit.getVozMarca(),
                LogoDisenoJson.desdeTexto(kit.getLogoDiseno()),
                simulado);
    }
}