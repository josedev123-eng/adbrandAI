package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.OpcionAnuncio;

// Una de las 4 opciones que ve la PYME: texto, imagen lista para <img src> y de dónde salió cada parte.
public record OpcionAnuncioResponse(
        Long id,
        int numero,
        String estilo,
        String texto,
        String imagen,
        String proveedorImagen,
        boolean imagenSimulada,
        boolean textoSimulado,
        boolean elegida) {

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
