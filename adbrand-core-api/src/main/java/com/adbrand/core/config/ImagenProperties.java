package com.adbrand.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Datos de las IA de imágenes. Se leen de application.properties, que a su vez los toma del .env.
// - mock: con true (por defecto) todas las imágenes son de prueba y no se llama a ninguna IA ni se gasta crédito.
// - Cloudflare Workers AI (gratis): hace los borradores.
// - OpenRouter (pagado, prepago): hace la versión final y los retoques.
@ConfigurationProperties(prefix = "ia.imagen")
public record ImagenProperties(
        boolean mock,
        String cloudflareAccountId,
        String cloudflareToken,
        String modeloBorrador,
        String openrouterApiKey,
        String modeloFinal,
        int timeoutSeconds) {

    public boolean cloudflareConfigurado() {
        return tieneTexto(cloudflareAccountId) && tieneTexto(cloudflareToken);
    }

    public boolean openrouterConfigurado() {
        return tieneTexto(openrouterApiKey);
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
