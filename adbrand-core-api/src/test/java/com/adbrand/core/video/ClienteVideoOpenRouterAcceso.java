package com.adbrand.core.video;

import java.math.BigDecimal;
import tools.jackson.databind.JsonNode;

// Acceso a la lógica de precios del cliente de OpenRouter (es de paquete) para las pruebas.
final class ClienteVideoOpenRouterAcceso {

    private ClienteVideoOpenRouterAcceso() {
    }

    static BigDecimal precioDe(JsonNode skus, String resolucion, boolean conAudio) {
        return com.adbrand.core.video.service.ClienteVideoOpenRouterPrecios.precioDe(skus, resolucion, conAudio);
    }
}
