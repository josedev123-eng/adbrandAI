package com.adbrand.core.brandkit.dto;

// La respuesta con el Kit de Marca generado.
public record KitMarcaGeneradoResponse(
        Long id,
        String nombreNegocio,
        String sector,
        String logoConcepto,
        String tipografiaPrimaria,
        String tipografiaSecundaria,
        String paletaColores,
        String vozMarca,
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
                simulado);
    }
}