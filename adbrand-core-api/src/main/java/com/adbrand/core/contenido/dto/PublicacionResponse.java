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
        boolean tieneWhatsapp,
        // HU 64: lo que conviene corregir para cumplir las reglas de publicidad (vacío si está todo bien).
        List<ObservacionLegal> observaciones,
        // HU 61: logo, colores y eslogan del kit para el afiche y lo que se comparte.
        MarcaPublicacion marca,
        // El video para Reels y Estados es de los planes de pago: false con el plan gratis (la web muestra el candado).
        boolean videoDisponible) {
}
