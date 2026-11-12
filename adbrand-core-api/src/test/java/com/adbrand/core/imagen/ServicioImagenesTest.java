package com.adbrand.core.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.config.ImagenProperties;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.GeneradorImagen;
import com.adbrand.core.imagen.service.GeneradorImagenSimulado;
import com.adbrand.core.imagen.service.ServicioImagenes;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

// Base del servicio de imágenes: modo simulado, qué IA se usa en cada paso y el respaldo si una falla.
class ServicioImagenesTest {

    private final GeneradorImagenSimulado simulado = new GeneradorImagenSimulado();
    private final GeneradorImagen cloudflare = generadorFalso(ServicioImagenes.CLOUDFLARE);
    private final GeneradorImagen openrouter = generadorFalso(ServicioImagenes.OPENROUTER);

    private static GeneradorImagen generadorFalso(String nombre) {
        GeneradorImagen generador = mock(GeneradorImagen.class);
        when(generador.nombre()).thenReturn(nombre);
        when(generador.generar(anyString(), any())).thenReturn(new ImagenGenerada("image/png", "AAAA", nombre, false));
        return generador;
    }

    private ServicioImagenes servicio(boolean mock) {
        ImagenProperties propiedades = new ImagenProperties(mock, "cuenta", "token", "flux", "clave", "gemini", 90);
        return new ServicioImagenes(propiedades, List.of(simulado, cloudflare, openrouter), simulado);
    }

    private static String svg(ImagenGenerada imagen) {
        return new String(Base64.getDecoder().decode(imagen.base64()), StandardCharsets.UTF_8);
    }

    @Test
    void enModoSimuladoNoLlamaANingunaIa() {
        ImagenGenerada imagen = servicio(true).borrador("Pan de yema recién horneado");

        assertThat(imagen.simulado()).isTrue();
        assertThat(imagen.proveedor()).isEqualTo("simulado");
        assertThat(imagen.dataUrl()).startsWith("data:image/svg+xml;base64,");
        assertThat(svg(imagen)).contains("Pan de yema recién horneado");
        verify(cloudflare, never()).generar(anyString(), any());
        verify(openrouter, never()).generar(anyString(), any());
    }

    @Test
    void elBorradorLoHaceCloudflare() {
        ImagenGenerada imagen = servicio(false).borrador("Pan de yema");

        assertThat(imagen.proveedor()).isEqualTo("cloudflare");
        verify(openrouter, never()).generar(anyString(), any());
    }

    @Test
    void laVersionFinalLaHaceOpenRouter() {
        ImagenGenerada imagen = servicio(false).finalORetoque("Pan de yema", null);

        assertThat(imagen.proveedor()).isEqualTo("openrouter");
        verify(cloudflare, never()).generar(anyString(), any());
    }

    @Test
    void siOpenRouterFallaLaFinalSaleDeCloudflare() {
        when(openrouter.generar(anyString(), any())).thenThrow(new IllegalStateException("sin crédito"));

        ImagenGenerada imagen = servicio(false).finalORetoque("Pan de yema", null);

        assertThat(imagen.proveedor()).isEqualTo("cloudflare");
    }

    @Test
    void siTodasLasIaFallanSeUsaLaImagenDePrueba() {
        when(cloudflare.generar(anyString(), any())).thenThrow(new IllegalStateException("límite diario"));

        ImagenGenerada imagen = servicio(false).borrador("Pan de yema");

        assertThat(imagen.simulado()).isTrue();
        assertThat(imagen.proveedor()).isEqualTo("simulado");
    }

    @Test
    void elRetoqueRecibeLaImagenAnterior() {
        ImagenGenerada anterior = new ImagenGenerada("image/png", "BBBB", "cloudflare", false);

        servicio(false).finalORetoque("pon el fondo más claro", anterior);

        verify(openrouter).generar("pon el fondo más claro", anterior);
    }

    @Test
    void laImagenDePruebaEscapaLosCaracteresEspecialesDelSvg() {
        ImagenGenerada imagen = simulado.generar("2x1 <hoy> & \"mañana\"", null);

        assertThat(svg(imagen)).contains("2x1 &lt;hoy&gt; &amp; &quot;mañana&quot;").doesNotContain("<hoy>");
    }
}
