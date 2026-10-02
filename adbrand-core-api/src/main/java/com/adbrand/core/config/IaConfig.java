package com.adbrand.core.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Crea el cliente HTTP que habla con el servidor de IA, con su dirección, clave y tiempo máximo de espera.
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {

    @Bean
    public RestClient restClientIa(IaProperties ia) {
        Duration espera = Duration.ofSeconds(ia.timeoutSeconds());
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(espera);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(ia.baseUrl() == null ? "" : ia.baseUrl())
                .requestFactory(new BufferingClientHttpRequestFactory(fabrica)); // envía el tamaño del cuerpo (Content-Length)
        if (ia.apiKey() != null && !ia.apiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ia.apiKey());
        }
        return builder.build();
    }
}