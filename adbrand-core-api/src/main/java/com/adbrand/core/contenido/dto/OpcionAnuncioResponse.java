package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.OpcionAnuncio;

// Una de las 4 opciones que ve la PYME: texto, imagen lista para <img src> y de dónde salió cada parte.
// "diseno": el anuncio ya diseñado (foto + titular + precio + botón + marca), JPG de 720 px; null si no se armó.
public record OpcionAnuncioResponse(
        Long id,
        int numero,
        String estilo,
        String texto,
        String imagen,
        String proveedorImagen,
        boolean imagenSimulada,
        boolean textoSimulado,
        boolean elegida,
        String diseno) {

    public OpcionAnuncioResponse(Long id, int numero, String estilo, String texto, String imagen, String proveedorImagen,
                                 boolean imagenSimulada, boolean textoSimulado, boolean elegida) {
        this(id, numero, estilo, texto, imagen, proveedorImagen, imagenSimulada, textoSimulado, elegida, null);
    }

    public OpcionAnuncioResponse conDiseno(String nuevo) {
        return new OpcionAnuncioResponse(id, numero, estilo, texto, imagen, proveedorImagen, imagenSimulada,
                textoSimulado, elegida, nuevo);
    }

    public static OpcionAnuncioResponse desde(OpcionAnuncio opcion) {
        return new OpcionAnuncioResponse(
                opcion.getId(),
                opcion.getNumero(),
                opcion.getEstilo(),
                opcion.getTexto(),
                opcion.imagen().dataUrl(),
                opcion.imagen().proveedor(),
                opcion.imagen().simulado(),
                opcion.isTextoSimulado(),
                opcion.isElegida());
    }
}
