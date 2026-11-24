package com.adbrand.core.imagen.dto;

// Una imagen hecha por alguna IA (o la de prueba): su tipo, su contenido en base64 y quién la hizo.
public record ImagenGenerada(String tipo, String base64, String proveedor, boolean simulado) {

    // Para mostrarla directo en la web con <img src="...">.
    public String dataUrl() {
        return "data:" + tipo + ";base64," + base64;
    }
}
