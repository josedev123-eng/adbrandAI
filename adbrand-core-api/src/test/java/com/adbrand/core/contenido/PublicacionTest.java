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

        // La marca primero; "Navidad" en el texto suma los de la festividad.
        assertThat(hashtags).startsWith("#PanaderiaDonaRosa", "#Navidad", "#NavidadPeruana", "#panaderia");
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

    @Test
    void losHashtagsSeAjustanALaFestividadYAlProducto() {
        List<String> hashtags = PublicacionService.hashtags("Cupcakes de calabaza para esta noche #Postres",
                "Cafetería Don Pepe", "Cafetería", "2x1 en cupcakes por Halloween");

        assertThat(hashtags).startsWith("#CafeteriaDonPepe", "#Halloween", "#HalloweenPeru");
        assertThat(hashtags).contains("#Postres", "#ComproLocal");
        assertThat(hashtags).hasSizeLessThanOrEqualTo(8);
    }

    @Test
    void comproLocalSiempreTieneSuLugarAunqueHayaMuchos() {
        List<String> hashtags = PublicacionService.hashtags("#Uno #Dos #Tres #Cuatro #Cinco #Seis #Siete",
                "Panadería Rosa", "Panadería", "Panetones por Navidad");

        assertThat(hashtags).hasSize(8).contains("#ComproLocal", "#HechoEnPeru", "#Navidad");
    }

    @Test
    void lasCondicionesDeIndecopiSeEscribenParaElDiseno() {
        assertThat(com.adbrand.core.contenido.service.DisenoAnuncio.condicionesDe("2026-12-24", "50"))
                .isEqualTo("Válido hasta el 24/12/2026 o hasta agotar stock (50 unidades).");
        assertThat(com.adbrand.core.contenido.service.DisenoAnuncio.condicionesDe(null, "")).isNull();
        assertThat(com.adbrand.core.contenido.service.DisenoAnuncio.condicionesDe("mañana", "x")).isNull();
    }
}
