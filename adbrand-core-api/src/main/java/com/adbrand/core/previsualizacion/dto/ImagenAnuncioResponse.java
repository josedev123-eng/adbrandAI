package com.adbrand.core.previsualizacion.dto;

// HU 46: imagen de un anuncio lista para mostrarse ("data:image/...;base64,..."). null si el anuncio no tiene imagen.
public record ImagenAnuncioResponse(Long contenidoId, String imagen) {
}
