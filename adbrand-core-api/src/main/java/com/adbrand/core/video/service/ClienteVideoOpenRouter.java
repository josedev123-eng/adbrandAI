package com.adbrand.core.video.service;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.config.VideoIaProperties;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// HU 66: generación de video de OpenRouter (misma clave que las imágenes, AI_IMAGE_API_KEY).
// Flujo: POST /videos (devuelve el id del trabajo) → GET /videos/{id} hasta "completed" → GET /videos/{id}/content.
// El precio por segundo sale de GET /videos/models (pricing_skus), para no gastar más del tope configurado.
@Component
public class ClienteVideoOpenRouter {

    static final String API = "https://openrouter.ai/api/v1";

    // Estado de un trabajo: status (pending, in_progress, completed, failed, cancelled, expired), avance y costo.
    public record EstadoTrabajo(String estado, Integer progreso, BigDecimal costo, String error) {
        public boolean terminado() {
            return List.of("completed", "failed", "cancelled", "expired").contains(estado);
        }

        public boolean completado() {
            return "completed".equals(estado);
        }
    }

    private final ImagenProperties imagen;
    private final VideoIaProperties video;
    private final RestClient cliente;
    private final ObjectMapper lector = new ObjectMapper();

    @Autowired
    public ClienteVideoOpenRouter(ImagenProperties imagen, VideoIaProperties video) {
        this(imagen, video, crearCliente());
    }

    ClienteVideoOpenRouter(ImagenProperties imagen, VideoIaProperties video, RestClient cliente) {
        this.imagen = imagen;
        this.video = video;
        this.cliente = cliente;
    }

    private static RestClient crearCliente() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(120));
        return RestClient.builder().baseUrl(API).requestFactory(fabrica).build();
    }

    public boolean configurado() {
        return imagen.openrouterConfigurado();
    }

    // Precio por segundo del modelo configurado en la resolución configurada (y sin o con audio). null si no se sabe.
    public BigDecimal precioPorSegundo() {
        String cuerpo = cliente.get().uri("/videos/models").retrieve().body(String.class);
        JsonNode modelos = lector.readTree(cuerpo == null ? "{}" : cuerpo).path("data");
        for (JsonNode modelo : modelos) {
            if (video.modelo().equals(modelo.path("id").asText())) {
                return precioDe(modelo.path("pricing_skus"), video.resolucion(), video.conAudio());
            }
        }
        return null;
    }

    // Elige el precio que corresponde: primero el de la resolución y el audio exactos; si no, el general.
    static BigDecimal precioDe(JsonNode skus, String resolucion, boolean conAudio) {
        return ClienteVideoOpenRouterPrecios.precioDe(skus, resolucion, conAudio);
    }

    // Crea el trabajo: la foto del anuncio como primer cuadro (data URL) y la instrucción de movimiento.
    public String crear(String instruccion, String fotoDataUrl) {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("model", video.modelo());
        pedido.put("prompt", instruccion);
        pedido.put("duration", video.duracionSegundos());
        pedido.put("resolution", video.resolucion());
        pedido.put("aspect_ratio", "9:16");
        pedido.put("generate_audio", video.conAudio());
        if (fotoDataUrl != null) {
            pedido.put("frame_images", List.of(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", fotoDataUrl),
                    "frame_type", "first_frame")));
        }
        String respuesta = cliente.post()
                .uri("/videos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + imagen.openrouterApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(pedido)
                .exchange((solicitud, r) -> {
                    String texto = new String(r.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (r.getStatusCode().isError()) {
                        throw new IllegalStateException("OpenRouter no aceptó el video (" + r.getStatusCode().value() + "): "
                                + mensajeDe(texto));
                    }
                    return texto;
                });
        return lector.readTree(respuesta).path("id").asText();
    }

    public EstadoTrabajo estado(String trabajoId) {
        String cuerpo = cliente.get()
                .uri("/videos/{id}", trabajoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + imagen.openrouterApiKey())
                .retrieve()
                .body(String.class);
        JsonNode json = lector.readTree(cuerpo == null ? "{}" : cuerpo);
        JsonNode costo = json.path("usage").path("cost");
        return new EstadoTrabajo(
                json.path("status").asText("pending"),
                json.path("progress").isNumber() ? json.path("progress").asInt() : null,
                costo.isNumber() ? costo.decimalValue() : null,
                json.path("error").isTextual() ? json.path("error").asText() : null);
    }

    public byte[] descargar(String trabajoId) {
        return cliente.get()
                .uri("/videos/{id}/content?index=0", trabajoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + imagen.openrouterApiKey())
                .retrieve()
                .body(byte[].class);
    }

    private String mensajeDe(String texto) {
        try {
            JsonNode json = lector.readTree(texto);
            JsonNode error = json.path("error");
            return error.isTextual() ? error.asText() : error.path("message").asText(texto);
        } catch (RuntimeException ex) {
            return texto.length() > 200 ? texto.substring(0, 200) : texto;
        }
    }
}
