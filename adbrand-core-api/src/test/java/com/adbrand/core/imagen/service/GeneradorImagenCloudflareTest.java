package com.adbrand.core.imagen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// Cloudflare Workers AI con un servidor simulado: qué se le manda y cómo se lee la imagen.
class GeneradorImagenCloudflareTest {

    private static final String URL = "https://api.cloudflare.com/client/v4/accounts/cuenta123/ai/run/@cf/black-forest-labs/flux-1-schnell";

    private MockRestServiceServer servidor;
    private GeneradorImagenCloudflare generador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeneradorImagenCloudflare.API);
        servidor = MockRestServiceServer.bindTo(builder).build();
        ImagenProperties propiedades = new ImagenProperties(false, "cuenta123", "token-secreto",
                "@cf/black-forest-labs/flux-1-schnell", "", "", 90);
        generador = new GeneradorImagenCloudflare(propiedades, builder.build());
    }

    @Test
    void mandaLaDescripcionConElTokenYLeeLaImagenDelJson() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-secreto"))
                .andExpect(content().json("{\"prompt\": \"pan de yema\", \"steps\": 4}"))
                .andRespond(withSuccess("{\"result\": {\"image\": \"/9j/AAAA\"}, \"success\": true}",
                        MediaType.APPLICATION_JSON));

        ImagenGenerada imagen = generador.generar("pan de yema", null);

        assertThat(imagen.tipo()).isEqualTo("image/jpeg");
        assertThat(imagen.base64()).isEqualTo("/9j/AAAA");
        assertThat(imagen.proveedor()).isEqualTo("cloudflare");
        assertThat(imagen.simulado()).isFalse();
        servidor.verify();
    }

    @Test
    void siElModeloDevuelveLaImagenDirectaLaPasaABase64() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, MediaType.IMAGE_PNG));

        ImagenGenerada imagen = generador.generar("pan de yema", null);

        assertThat(imagen.tipo()).isEqualTo("image/png");
        assertThat(imagen.base64()).isEqualTo("AQID");
    }

    @Test
    void unErrorDeCloudflareSeAvisaConSuCodigo() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"errors\": [\"limite diario\"]}"));

        assertThatThrownBy(() -> generador.generar("pan de yema", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("429");
    }

    @Test
    void sinCuentaOTokenNoLlamaACloudflare() {
        GeneradorImagenCloudflare sinConfigurar = new GeneradorImagenCloudflare(
                new ImagenProperties(false, "", "", "flux", "", "", 90), RestClient.create());

        assertThatThrownBy(() -> sinConfigurar.generar("pan de yema", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CF_ACCOUNT_ID");
    }
}
