package com.adbrand.core.estado.service;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.estado.dto.EstadoIa;
import com.adbrand.core.estado.dto.EstadoResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// HU 18: revisa si el backend y el servidor de IA están activos para el panel del administrador técnico.
@Service
public class EstadoService {

    private static final Logger log = LoggerFactory.getLogger(EstadoService.class);

    public static final String ACTIVO = "ACTIVO";
    public static final String CAIDO = "CAIDO";
    static final Duration ESPERA = Duration.ofSeconds(5);

    private final IaProperties ia;
    private final HttpClient http;

    public EstadoService(IaProperties ia) {
        this.ia = ia;
        this.http = HttpClient.newBuilder().connectTimeout(ESPERA).build();
    }

    public EstadoResponse verificar() {
        // Si este código se ejecuta, el backend está respondiendo.
        return new EstadoResponse(ACTIVO, verificarIa());
    }

    // Solo pregunta si el servidor de IA contesta: no llama al chat, así no gasta tokens.
    // No manda la clave (AI_API_KEY): cualquier respuesta HTTP, aunque sea 401 o 404, significa que está encendido.
    public EstadoIa verificarIa() {
        if (ia.mock()) {
            return new EstadoIa(ACTIVO, 0L, "Modo simulado");
        }
        if (ia.baseUrl() == null || ia.baseUrl().isBlank()) {
            return new EstadoIa(CAIDO, null, "Falta configurar AI_BASE_URL");
        }

        long inicio = System.nanoTime();
        try {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(ia.baseUrl())).timeout(ESPERA).GET().build();
            http.send(pedido, HttpResponse.BodyHandlers.discarding());
            return new EstadoIa(ACTIVO, Duration.ofNanos(System.nanoTime() - inicio).toMillis(), null);
        } catch (HttpTimeoutException ex) {
            return caido("La IA no respondió en " + ESPERA.toSeconds() + " segundos");
        } catch (IOException | IllegalArgumentException ex) {
            return caido("No se pudo conectar con la IA (" + ex.getClass().getSimpleName() + ")");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return caido("Se interrumpió la verificación de la IA");
        }
    }

    private EstadoIa caido(String detalle) {
        log.warn("Servidor de IA caído: {}", detalle);
        return new EstadoIa(CAIDO, null, detalle);
    }
}
