package com.adbrand.core.imagen.dto;

// Lo que recibe la web: la imagen lista para <img src>, qué IA la hizo y si es de prueba.
public record ImagenResponse(String imagen, String proveedor, boolean simulado) {

    public static ImagenResponse desde(ImagenGenerada imagen) {
        return new ImagenResponse(imagen.dataUrl(), imagen.proveedor(), imagen.simulado());
    }
}
