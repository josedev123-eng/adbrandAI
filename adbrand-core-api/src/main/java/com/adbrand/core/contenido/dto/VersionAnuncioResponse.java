package com.adbrand.core.contenido.dto;

import com.adbrand.core.contenido.entity.VersionAnuncio;

// Una versión del historial: su número (v1, v2…), qué se pidió cambiar y su imagen.
public record VersionAnuncioResponse(int numero, String instruccion, String imagen, String proveedorImagen,
                                     boolean imagenSimulada, boolean actual) {

    public static VersionAnuncioResponse desde(VersionAnuncio version, int numeroActual) {
        return new VersionAnuncioResponse(
                version.getNumero(),
                version.getInstruccion(),
                version.imagen().dataUrl(),
                version.imagen().proveedor(),
                version.imagen().simulado(),
                version.getNumero() == numeroActual);
    }
}
