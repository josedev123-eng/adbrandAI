package com.adbrand.core.contenido.dto;

import com.adbrand.core.negocio.entity.Tono;

// El anuncio listo para copiar. "simulado" avisa a la web si vino del modo de prueba.
public record AnuncioGeneradoResponse(String texto, RedSocial redSocial, Tono tono, boolean simulado) {
}