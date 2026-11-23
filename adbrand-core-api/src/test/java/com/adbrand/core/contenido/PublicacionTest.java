package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.service.PublicacionService;
import java.util.List;
import org.junit.jupiter.api.Test;

// Listo para publicar: hashtags de marca + rubro + Perú, texto limpio y enlace de WhatsApp solo para celulares.
class PublicacionTest {

    @Test
    void armaLosHashtagsSinRepetirYConLaMarcaPrimero() {
        List<String> hashtags = PublicacionService.hashtags(
                "¡2x1 en panetones! #Navidad #panaderia", "Panadería Doña Rosa", "Panadería y pastelería");

        assertThat(hashtags).startsWith("#PanaderiaDonaRosa", "#Navidad", "#panaderia");
        assertThat(hashtags).contains("#PanCaliente", "#ComproLocal", "#HechoEnPeru");
        // "#Panaderia" del rubro no se repite porque ya vino "#panaderia" en el texto.
        assertThat(hashtags.stream().filter(h -> h.equalsIgnoreCase("#panaderia"))).hasSize(1);
        assertThat(hashtags).hasSizeLessThanOrEqualTo(8);
    }

    @Test
    void elNombreSeVuelveUnHashtagSinTildesNiEspacios() {
        assertThat(PublicacionService.hashtagDe("Cafetería Don Pepe")).isEqualTo("#CafeteriaDonPepe");
        assertThat(PublicacionService.hashtagDe("  ")).isNull();
    }

    @Test
    void sacaLosHashtagsDelTextoParaPonerlosOrdenadosAlFinal() {
        assertThat(PublicacionService.sinHashtags("Ven hoy #Promo\n\n\n#Lima #Peru")).isEqualTo("Ven hoy");
    }

    @Test
    void soloUnCelularPeruanoTieneEnlaceDeWhatsapp() {
        assertThat(PublicacionService.celularWhatsapp("987 654 321")).isEqualTo("51987654321");
        assertThat(PublicacionService.celularWhatsapp("+51 987654321")).isEqualTo("51987654321");
        assertThat(PublicacionService.celularWhatsapp("4251234")).isNull();
        assertThat(PublicacionService.celularWhatsapp(null)).isNull();
    }
}
