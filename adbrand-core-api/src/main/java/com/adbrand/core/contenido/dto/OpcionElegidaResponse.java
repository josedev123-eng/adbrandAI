package com.adbrand.core.contenido.dto;

// Al elegir una opción: el anuncio guardado (con el resultado del filtro de la HU 13) y la opción con su imagen.
public record OpcionElegidaResponse(AnuncioGeneradoResponse anuncio, OpcionAnuncioResponse opcion) {
}
