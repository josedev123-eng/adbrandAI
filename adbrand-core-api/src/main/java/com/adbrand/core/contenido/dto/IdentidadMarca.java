package com.adbrand.core.contenido.dto;

import com.adbrand.core.brandkit.entity.KitMarca;

// HU 31: la identidad guardada en el kit de marca que el anuncio debe respetar.
public record IdentidadMarca(String tipografiaPrimaria, String tipografiaSecundaria, String vozMarca) {

    public static IdentidadMarca desde(KitMarca kit) {
        return new IdentidadMarca(kit.getTipografiaPrimaria(), kit.getTipografiaSecundaria(), kit.getVozMarca());
    }

    public boolean tieneVoz() {
        return vozMarca != null && !vozMarca.isBlank();
    }
}
