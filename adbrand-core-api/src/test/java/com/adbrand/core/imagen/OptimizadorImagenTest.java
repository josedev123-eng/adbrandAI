package com.adbrand.core.imagen;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.OptimizadorImagen;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

// Rendimiento: las imágenes pesadas de la IA se guardan como JPEG liviano de máximo 1024 px.
class OptimizadorImagenTest {

    // Un PNG con ruido de colores: no se comprime bien, así que pesa como una foto real de la IA.
    private static String pngPesado(int lado) throws Exception {
        BufferedImage imagen = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Random azar = new Random(7);
        for (int x = 0; x < lado; x++) {
            for (int y = 0; y < lado; y++) {
                imagen.setRGB(x, y, azar.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return Base64.getEncoder().encodeToString(salida.toByteArray());
    }

    @Test
    void unPngPesadoSePasaAJpegMasLivianoYDeMaximo1024px() throws Exception {
        String original = pngPesado(1400);
        ImagenGenerada imagen = new ImagenGenerada("image/png", original, "openrouter", false);

        ImagenGenerada optimizada = OptimizadorImagen.optimizar(imagen);

        assertThat(optimizada.tipo()).isEqualTo("image/jpeg");
        assertThat(optimizada.base64().length()).isLessThan(original.length());
        assertThat(optimizada.proveedor()).isEqualTo("openrouter");
        BufferedImage leida = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(optimizada.base64())));
        assertThat(Math.max(leida.getWidth(), leida.getHeight())).isEqualTo(1024);
    }

    @Test
    void unaImagenLivianaOLaDePruebaEnSvgNoSeTocan() throws Exception {
        ImagenGenerada liviana = new ImagenGenerada("image/png", pngPesado(40), "cloudflare", false);
        ImagenGenerada svg = new ImagenGenerada("image/svg+xml", "PHN2Zy8+", "simulado", true);

        assertThat(OptimizadorImagen.optimizar(liviana)).isSameAs(liviana);
        assertThat(OptimizadorImagen.optimizar(svg)).isSameAs(svg);
    }

    @Test
    void siLaImagenNoSePuedeLeerSeUsaLaOriginal() {
        byte[] basura = new byte[300 * 1024];
        new Random(3).nextBytes(basura);
        ImagenGenerada rota = new ImagenGenerada("image/png", Base64.getEncoder().encodeToString(basura), "openrouter", false);

        assertThat(OptimizadorImagen.optimizar(rota)).isSameAs(rota);
    }
}
