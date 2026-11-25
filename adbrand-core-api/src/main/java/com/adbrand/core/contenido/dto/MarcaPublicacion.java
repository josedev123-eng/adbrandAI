package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.service.MarcaDelNegocio;

// HU 61: la marca que acompaña al anuncio listo para publicar (para el afiche con QR y lo que se comparte):
// nombre, eslogan (null si no eligió), logo como data URL (null si no tiene) y los colores principal y de acento.
public record MarcaPublicacion(String nombre, String eslogan, String logo, String colorPrincipal, String colorAcento) {

    public static MarcaPublicacion desde(MarcaDelNegocio.Marca marca) {
        return new MarcaPublicacion(marca.nombre(), marca.eslogan(), marca.logoDataUrl(),
                MarcaDelNegocio.hex(marca.principal()), MarcaDelNegocio.hex(marca.acento()));
    }
}
