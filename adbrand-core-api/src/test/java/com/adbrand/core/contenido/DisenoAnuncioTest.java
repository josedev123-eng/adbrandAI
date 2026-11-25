package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.service.DisenoAnuncio;
import com.adbrand.core.contenido.service.DisenoAnuncio.Plantilla;
import com.adbrand.core.contenido.service.DisenoAnuncio.Textos;
import com.adbrand.core.contenido.service.MarcaDelNegocio.Marca;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

// Ajuste 14: las 4 opciones son anuncios diseñados (titular, etiqueta con el precio, botón y marca), no letras
// encima de una foto. Cada opción usa una plantilla distinta y todas funcionan en cualquier formato.
class DisenoAnuncioTest {

    private static final Color CAFE = new Color(0x8B, 0x45, 0x13);
    private static final Color DORADO = new Color(0xF2, 0xC1, 0x4E);

    private static BufferedImage foto() {
        BufferedImage imagen = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        var g = imagen.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 512, 512);
        g.dispose();
        return imagen;
    }

    @Test
    void sacaElPrecioALaEtiquetaYLoQuitaDelTitular() {
        Textos t = DisenoAnuncio.textosDe("2 panetones por S/ 35 hasta el 24 de diciembre", "", true);

        assertThat(t.etiqueta()).isEqualTo("S/ 35");
        assertThat(t.titular()).isEqualTo("2 panetones hasta el 24 de diciembre");
        assertThat(t.llamado()).isEqualTo("Pide por WhatsApp");
    }

    @Test
    void reconoceLasPromosYLosDescuentos() {
        assertThat(DisenoAnuncio.textosDe("2x1 en cafés americanos", "", false).etiqueta()).isEqualTo("2X1");
        assertThat(DisenoAnuncio.textosDe("30% en toda la tienda", "", false).etiqueta()).isEqualTo("-30%");
        assertThat(DisenoAnuncio.textosDe("Pan recién horneado", "", false).etiqueta()).isNull();
        assertThat(DisenoAnuncio.textosDe("Pan", "", false).llamado()).isEqualTo("¡Te esperamos!");
    }

    @Test
    void cadaOpcionUsaUnaPlantillaDistinta() {
        assertThat(Plantilla.deOpcion(1)).isEqualTo(Plantilla.TITULAR);
        assertThat(Plantilla.deOpcion(2)).isEqualTo(Plantilla.BLOQUE);
        assertThat(Plantilla.deOpcion(3)).isEqualTo(Plantilla.MINIMAL);
        assertThat(Plantilla.deOpcion(4)).isEqualTo(Plantilla.DIVIDIDO);
    }

    @Test
    void todasLasPlantillasSeDibujanEnLosTresFormatosConYSinFoto() {
        Marca marca = new Marca("Panadería Rosa", "El sabor de tu barrio", null, null, CAFE, DORADO);
        Textos textos = DisenoAnuncio.textosDe("2 panetones por S/ 35", "", true);
        int[][] formatos = {{1080, 1080}, {1080, 1920}, {1200, 628}};
        for (Plantilla plantilla : Plantilla.values()) {
            for (int[] f : formatos) {
                BufferedImage conFoto = DisenoAnuncio.dibujar(foto(), textos, marca, plantilla, f[0], f[1]);
                BufferedImage sinFoto = DisenoAnuncio.dibujar(null, textos, marca, plantilla, f[0], f[1]);
                assertThat(conFoto.getWidth()).isEqualTo(f[0]);
                assertThat(conFoto.getHeight()).isEqualTo(f[1]);
                assertThat(sinFoto.getHeight()).isEqualTo(f[1]);
            }
        }
    }

    @Test
    void laPlantillaTitularPoneLaFranjaDeColorArriba() {
        Marca marca = new Marca("Panadería Rosa", null, null, null, CAFE, DORADO);

        BufferedImage imagen = DisenoAnuncio.dibujar(foto(), DisenoAnuncio.textosDe("Pan", "", false), marca,
                Plantilla.TITULAR, 1080, 1080);

        Color arriba = new Color(imagen.getRGB(5, 5));
        assertThat(Math.abs(arriba.getRed() - CAFE.getRed())).isLessThan(6);
        assertThat(new Color(imagen.getRGB(540, 600))).isEqualTo(Color.BLUE);
    }
}
