package com.adbrand.core.brandkit.dto;

// La respuesta con el Kit de Marca generado. logoDiseno es el logo del editor (HU 29), o null si aún no hay.
// Identidad de marca (V70): eslogan elegido y sus opciones, el logo como imagen (data URL) y de dónde salió.
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
        boolean simulado,
        String sentimiento,
        String diferencial,
        String eslogan,
        java.util.List<String> esloganOpciones,
        String logoImagen,
        String logoOrigen) {

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
                simulado,
                kit.getSentimiento(),
                kit.getDiferencial(),
                kit.getEslogan(),
                kit.getEsloganOpciones() == null ? java.util.List.of()
                        : kit.getEsloganOpciones().lines().map(String::trim).filter(s -> !s.isEmpty()).toList(),
                kit.logoImagenDataUrl(),
                kit.getLogoOrigen());
    }
}