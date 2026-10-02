package com.adbrand.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Datos del servidor de IA. Se leen de application.properties, que a su vez los toma del .env.
@ConfigurationProperties(prefix = "ia")
public record IaProperties(
        String baseUrl,
        String apiKey,
        String model,
        String chatPath,
        int timeoutSeconds,
        boolean mock) {
}