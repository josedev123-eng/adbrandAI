package com.adbrand.core.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

// HU 66: video con IA (OpenRouter). Se lee de application.properties, que lo toma del .env.
// - modelo: por defecto google/veo-3.1-lite (el más barato con buena calidad a 720p).
// - costoMaximo: tope en dólares por video; si el modelo cuesta más, no se genera.
// - limiteMes: videos con IA por empresa al mes. plan: el plan que los incluye (Premium).
@ConfigurationProperties(prefix = "ia.video")
public record VideoIaProperties(
        String modelo,
        String resolucion,
        int duracionSegundos,
        boolean conAudio,
        BigDecimal costoMaximo,
        int limiteMes,
        String plan) {
}
