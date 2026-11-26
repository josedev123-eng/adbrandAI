package com.adbrand.core.video.service;

import java.math.BigDecimal;
import java.util.List;
import tools.jackson.databind.JsonNode;

// HU 66: elige el precio por segundo que corresponde de "pricing_skus" de OpenRouter: primero el de la resolución y
// el audio exactos ("duration_seconds_without_audio_720p"), luego el de la resolución, el del audio y el general.
public final class ClienteVideoOpenRouterPrecios {

    private ClienteVideoOpenRouterPrecios() {
    }

    public static BigDecimal precioDe(JsonNode skus, String resolucion, boolean conAudio) {
        String audio = conAudio ? "with_audio" : "without_audio";
        List<String> claves = List.of(
                "duration_seconds_" + audio + "_" + resolucion,
                "duration_seconds_" + resolucion,
                "duration_seconds_" + audio,
                "duration_seconds");
        for (String clave : claves) {
            JsonNode valor = skus.path(clave);
            if (!valor.isMissingNode() && !valor.isNull()) {
                return new BigDecimal(valor.asText());
            }
        }
        return null;
    }
}
