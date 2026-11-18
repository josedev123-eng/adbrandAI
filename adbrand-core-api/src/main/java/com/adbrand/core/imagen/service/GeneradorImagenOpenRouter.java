package com.adbrand.core.imagen.service;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
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

// Versión final y retoques con OpenRouter (prepago, usa el crédito de la clave AI_IMAGE_API_KEY).
// Usa un modelo de imágenes de Gemini que puede EDITAR: se le manda la imagen actual + la instrucción corta.
// Docs: POST https://openrouter.ai/api/v1/chat/completions con "modalities": ["image", "text"];
// la imagen vuelve en choices[0].message.images[0].image_url.url como "data:image/png;base64,...".
@Component
public class GeneradorImagenOpenRouter implements GeneradorImagen {

    static final String API = "https://openrouter.ai/api/v1";

    private final ImagenProperties propiedades;
    private final RestClient cliente;
    private final ObjectMapper lector = new ObjectMapper();

    @Autowired
    public GeneradorImagenOpenRouter(ImagenProperties propiedades) {
        this(propiedades, crearCliente(propiedades));
    }

    // Las pruebas pasan su propio RestClient (con un servidor simulado).
    GeneradorImagenOpenRouter(ImagenProperties propiedades, RestClient cliente) {
        this.propiedades = propiedades;
        this.cliente = cliente;
    }

    private static RestClient crearCliente(ImagenProperties propiedades) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(Math.max(propiedades.timeoutSeconds(), 10)));
        return RestClient.builder().baseUrl(API).requestFactory(fabrica).build();
    }

    @Override
    public String nombre() {
        return ServicioImagenes.OPENROUTER;
    }

    @Override
    public ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase) {
        if (!propiedades.openrouterConfigurado()) {
            throw new IllegalStateException("Falta AI_IMAGE_API_KEY en el .env");
        }
        List<Map<String, Object>> contenido = new ArrayList<>();
        contenido.add(Map.of("type", "text", "text", descripcion));
        // La imagen de prueba es un SVG y los modelos de imagen no la aceptan; en ese caso se crea desde cero.
        if (imagenBase != null && !imagenBase.tipo().contains("svg")) {
            contenido.add(Map.of("type", "image_url", "image_url", Map.of("url", imagenBase.dataUrl())));
        }
        Map<String, Object> pedido = Map.of(
                "model", propiedades.modeloFinal(),
                "modalities", List.of("image", "text"),
                "messages", List.of(Map.of("role", "user", "content", contenido)));

        return cliente.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.openrouterApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(pedido)
                .exchange((solicitud, respuesta) -> {
                    byte[] cuerpo = respuesta.getBody().readAllBytes();
                    String texto = new String(cuerpo, StandardCharsets.UTF_8);
                    if (respuesta.getStatusCode().isError()) {
                        // 402 = sin crédito; 429 = demasiadas peticiones. ServicioImagenes usará el respaldo.
                        throw new IllegalStateException("OpenRouter respondió " + respuesta.getStatusCode().value()
                                + ": " + recortar(texto));
                    }
                    return desdeJson(texto);
                });
    }

    private ImagenGenerada desdeJson(String texto) {
        JsonNode url = lector.readTree(texto).path("choices").path(0).path("message")
                .path("images").path(0).path("image_url").path("url");
        String dataUrl = url.isMissingNode() ? "" : url.asText();
        int coma = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:") || coma < 0) {
            throw new IllegalStateException("OpenRouter no devolvió ninguna imagen: " + recortar(texto));
        }
        String tipo = dataUrl.substring("data:".length(), dataUrl.indexOf(';') > 0 ? dataUrl.indexOf(';') : coma);
        return new ImagenGenerada(tipo, dataUrl.substring(coma + 1), ServicioImagenes.OPENROUTER, false);
    }

    private static String recortar(String texto) {
        return texto.length() <= 300 ? texto : texto.substring(0, 300) + "…";
    }
}
