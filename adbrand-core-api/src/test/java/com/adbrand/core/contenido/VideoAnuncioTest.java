package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.service.VideoAnuncioService;
import com.adbrand.core.contenido.service.VideoAnuncioService.Guion;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

// HU 65: el video del anuncio es un MP4 de verdad, con o sin foto y con o sin WhatsApp.
class VideoAnuncioTest {

    private static BufferedImage foto() {
        BufferedImage imagen = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        var g = imagen.createGraphics();
        g.setColor(new Color(0x8B, 0x45, 0x13));
        g.fillRect(0, 0, 512, 512);
        g.dispose();
        return imagen;
    }

    // Los MP4 llevan la caja "ftyp" al inicio del archivo.
    private static boolean esMp4(byte[] video) {
        return video.length > 1000 && new String(Arrays.copyOfRange(video, 4, 8), StandardCharsets.US_ASCII).equals("ftyp");
    }

    @Test
    void conFotoLogoYWhatsappGeneraUnMp4() {
        Guion guion = new Guion(foto(), "2 panetones por S/ 35", "Panadería Rosa", "El sabor de tu barrio",
                foto(), new Color(0x8B, 0x45, 0x13), "+51 987654321");

        assertThat(esMp4(VideoAnuncioService.codificar(guion))).isTrue();
    }

    @Test
    void sinFotoNiLogoNiWhatsappTambienSale() {
        Guion guion = new Guion(null, "Pan recién horneado", "Bodega Luis", null, null, new Color(0x4F, 0x46, 0xE5), null);

        assertThat(esMp4(VideoAnuncioService.codificar(guion))).isTrue();
    }
}
