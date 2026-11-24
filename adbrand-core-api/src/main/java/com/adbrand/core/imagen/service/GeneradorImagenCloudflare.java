package com.adbrand.core.imagen.service;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Borradores con Cloudflare Workers AI (plan gratis). Solo crea imágenes desde una descripción;
// los retoques sobre una imagen anterior los hace OpenRouter.
// Docs: POST https://api.cloudflare.com/client/v4/accounts/{cuenta}/ai/run/{modelo}
@Component
public class GeneradorImagenCloudflare implements GeneradorImagen {

    static final String API = "https://api.cloudflare.com/client/v4";

    private final ImagenProperties propiedades;
    private final RestClient cliente;
    private final ObjectMapper lector = new ObjectMapper();

    @Autowired
    public GeneradorImagenCloudflare(ImagenProperties propiedades) {
        this(propiedades, crearCliente(propiedades));
    }

    // Las pruebas pasan su propio RestClient (con un servidor simulado).
    GeneradorImagenCloudflare(ImagenProperties propiedades, RestClient cliente) {
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
        return ServicioImagenes.CLOUDFLARE;
    }

    @Override
    public ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase) {
        if (!propiedades.cloudflareConfigurado()) {
            throw new IllegalStateException("Faltan CF_ACCOUNT_ID o CF_API_TOKEN en el .env");
        }
        return cliente.post()
                // El modelo lleva "/" y "@" (ej. @cf/black-forest-labs/flux-1-schnell) que deben ir tal cual en la ruta,
                // por eso no se pasa como variable (se codificaría como %2F y Cloudflare no lo encontraría).
                .uri("/accounts/" + propiedades.cloudflareAccountId().strip() + "/ai/run/" + propiedades.modeloBorrador().strip())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.cloudflareToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("prompt", descripcion, "steps", 4))
                .exchange((pedido, respuesta) -> {
                    byte[] cuerpo = respuesta.getBody().readAllBytes();
                    if (respuesta.getStatusCode().isError()) {
                        throw new IllegalStateException("Cloudflare respondió " + respuesta.getStatusCode().value()
                                + ": " + recortar(new String(cuerpo, StandardCharsets.UTF_8)));
                    }
                    MediaType tipo = respuesta.getHeaders().getContentType();
                    // Algunos modelos (Stable Diffusion) devuelven la imagen directa; FLUX la manda en JSON.
                    if (tipo != null && "image".equals(tipo.getType())) {
                        return imagen(tipo.toString(), Base64.getEncoder().encodeToString(cuerpo));
                    }
                    return desdeJson(cuerpo);
                });
    }

    private ImagenGenerada desdeJson(byte[] cuerpo) throws IOException {
        JsonNode raiz = lector.readTree(cuerpo);
        JsonNode base64 = raiz.path("result").path("image");
        if (base64.isMissingNode() || base64.asText().isBlank()) {
            throw new IllegalStateException("Cloudflare no devolvió ninguna imagen: "
                    + recortar(new String(cuerpo, StandardCharsets.UTF_8)));
        }
        return imagen("image/jpeg", base64.asText());
    }

    private ImagenGenerada imagen(String tipo, String base64) {
        return new ImagenGenerada(tipo, base64, ServicioImagenes.CLOUDFLARE, false);
    }

    private static String recortar(String texto) {
        return texto.length() <= 300 ? texto : texto.substring(0, 300) + "…";
    }
}
