package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.service.ImagenFinalService;
import com.adbrand.core.contenido.service.MarcaDelNegocio;
import com.adbrand.core.contenido.service.MarcaDelNegocio.Marca;
import com.adbrand.core.contenido.service.PublicacionService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

// HU 61: los anuncios llevan el logo, los colores y el eslogan del kit de marca.
class ImagenFinalTest {

    private static final Color CAFE = new Color(0x8B, 0x45, 0x13);
    private static final Color DORADO = new Color(0xF2, 0xC1, 0x4E);

    private static BufferedImage cuadro(Color color, int lado) {
        BufferedImage imagen = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        var g = imagen.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, lado, lado);
        g.dispose();
        return imagen;
    }

    private static Color en(BufferedImage imagen, int x, int y) {
        return new Color(imagen.getRGB(x, y));
    }

    @Test
    void laImagenTieneElTamanoDelFormatoYLaFranjaConLosColoresDeLaMarca() {
        Marca marca = new Marca("Panadería Rosa", "El sabor de tu barrio", cuadro(Color.RED, 200), null, CAFE, DORADO);

        BufferedImage imagen = ImagenFinalService.dibujar(cuadro(Color.BLUE, 512), "2 panetones por S/ 35", marca, 1080, 1920);

        assertThat(imagen.getWidth()).isEqualTo(1080);
        assertThat(imagen.getHeight()).isEqualTo(1920);
        // Arriba la foto; abajo a la derecha la franja del color principal; justo en el borde, la línea de acento.
        assertThat(en(imagen, 540, 100)).isEqualTo(Color.BLUE);
        assertThat(en(imagen, 1070, 1910)).isEqualTo(CAFE);
        assertThat(en(imagen, 1070, 1920 - (int) (1080 * 0.17) + 1)).isEqualTo(DORADO);
    }

    @Test
    void elLogoDelKitVaEnLaFranja() {
        int franja = (int) (1080 * 0.17);
        Marca marca = new Marca("Bodega Luis", null, cuadro(Color.RED, 200), null, CAFE, DORADO);

        BufferedImage imagen = ImagenFinalService.dibujar(null, "Pan recién horneado", marca, 1080, 1080);

        // Centro del círculo del logo: margen (5 % del ancho) + medio diámetro.
        int x = (int) (1080 * 0.05) + (int) (franja * 0.68) / 2;
        int y = 1080 - franja + franja / 2 + franja / 36;
        Color centro = en(imagen, x, y);
        assertThat(centro.getRed()).isGreaterThan(200);
        assertThat(centro.getGreen()).isLessThan(60);
    }

    @Test
    void sinFotoNiLogoUsaElColorDeLaMarcaYLasIniciales() {
        Marca marca = new Marca("Bodega Luis", null, null, null, CAFE, DORADO);

        BufferedImage imagen = ImagenFinalService.dibujar(null, null, marca, 1200, 628);

        assertThat(imagen.getWidth()).isEqualTo(1200);
        // Arriba a la izquierda empieza el degradado del color principal.
        Color esquina = en(imagen, 0, 0);
        assertThat(Math.abs(esquina.getRed() - CAFE.getRed())).isLessThan(4);
        assertThat(Math.abs(esquina.getBlue() - CAFE.getBlue())).isLessThan(4);
        assertThat(marca.iniciales()).isEqualTo("BL");
    }

    @Test
    void leeLosColoresDeLaPaletaYEligeElTextoQueSeLee() {
        assertThat(MarcaDelNegocio.colores("Café #8B4513, dorado #F2C14E y otra vez #8b4513"))
                .containsExactly(CAFE, DORADO);
        assertThat(MarcaDelNegocio.hex(CAFE)).isEqualTo("#8B4513");
        assertThat(MarcaDelNegocio.textoSobre(CAFE)).isEqualTo(Color.WHITE);
        assertThat(MarcaDelNegocio.textoSobre(DORADO)).isNotEqualTo(Color.WHITE);
    }

    @Test
    void loQueSeComparteLlevaLaFirmaConElEslogan() {
        assertThat(PublicacionService.firma("Panadería Rosa", "“El sabor de tu barrio”"))
                .isEqualTo("— Panadería Rosa: “El sabor de tu barrio”");
        assertThat(PublicacionService.firma("Panadería Rosa", " ")).isNull();
    }
}
