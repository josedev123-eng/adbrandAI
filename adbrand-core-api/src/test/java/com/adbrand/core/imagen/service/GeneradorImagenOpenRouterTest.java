package com.adbrand.core.imagen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// OpenRouter con un servidor simulado: manda la imagen actual + la instrucción y lee la imagen editada.
class GeneradorImagenOpenRouterTest {

    private static final String URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String RESPUESTA = """
            {"choices": [{"message": {"role": "assistant", "content": "",
              "images": [{"type": "image_url", "image_url": {"url": "data:image/png;base64,EDITADA"}}]}}]}""";

    private MockRestServiceServer servidor;
    private GeneradorImagenOpenRouter generador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GeneradorImagenOpenRouter.API);
        servidor = MockRestServiceServer.bindTo(builder).build();
        generador = new GeneradorImagenOpenRouter(new ImagenProperties(false, "", "", "", "sk-or-prueba",
                "google/gemini-flash-image", 90), builder.build());
    }

    @Test
    void mandaLaImagenActualYLaInstruccionYLeeLaImagenEditada() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-or-prueba"))
                .andExpect(jsonPath("$.model").value("google/gemini-flash-image"))
                .andExpect(jsonPath("$.modalities", Matchers.contains("image", "text")))
                .andExpect(jsonPath("$.messages[0].content[0].text").value("fondo más claro"))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url").value("data:image/jpeg;base64,ORIGINAL"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        ImagenGenerada imagen = generador.generar("fondo más claro",
                new ImagenGenerada("image/jpeg", "ORIGINAL", "cloudflare", false));

        assertThat(imagen.tipo()).isEqualTo("image/png");
        assertThat(imagen.base64()).isEqualTo("EDITADA");
        assertThat(imagen.proveedor()).isEqualTo("openrouter");
        servidor.verify();
    }

    @Test
    void laImagenDePruebaSvgNoSeManda() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.messages[0].content.length()").value(1))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        generador.generar("fondo más claro", new ImagenGenerada("image/svg+xml", "PHN2Zz4=", "simulado", true));

        servidor.verify();
    }

    @Test
    void sinCreditoAvisaConElCodigo402() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).body("{\"error\": {\"message\": \"Insufficient credits\"}}"));

        assertThatThrownBy(() -> generador.generar("fondo", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("402");
    }

    @Test
    void siNoVuelveNingunaImagenAvisa() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"choices\": [{\"message\": {\"content\": \"No puedo\"}}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generador.generar("fondo", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ninguna imagen");
    }

    @Test
    void sinClaveNoLlamaAOpenRouter() {
        GeneradorImagenOpenRouter sinClave = new GeneradorImagenOpenRouter(
                new ImagenProperties(false, "", "", "", "", "modelo", 90), RestClient.create());

        assertThatThrownBy(() -> sinClave.generar("fondo", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_IMAGE_API_KEY");
    }
}
