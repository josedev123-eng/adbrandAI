package com.adbrand.core.contenido.dto;

import java.util.List;

// Un anuncio listo para publicar: el texto completo (con el enlace de pedido y los hashtags) para copiar y pegar,
// los hashtags sueltos, y el enlace "Pedir por WhatsApp" (null si la empresa no registró un celular).
public record PublicacionResponse(
        Long id,
        String textoParaPublicar,
        List<String> hashtags,
        String enlaceWhatsapp,
        String mensajePedido,
        boolean tieneWhatsapp) {
}
