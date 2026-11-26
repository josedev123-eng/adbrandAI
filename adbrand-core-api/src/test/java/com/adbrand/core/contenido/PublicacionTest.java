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

    // HU 62: la oferta en corto para destacarla en el afiche.
    @Test
    void laOfertaDestacadaDelAficheEsCortaYConLaCifra() {
        assertThat(PublicacionService.ofertaDestacada("20% de descuento en todos los cafés este fin de semana"))
                .isEqualTo("-20% en todos los cafés este fin de semana");
        assertThat(PublicacionService.ofertaDestacada("Promo 2 x 1 en pan de yema, solo hoy")).isEqualTo("2x1 en pan de yema");
        assertThat(PublicacionService.ofertaDestacada("Panetón artesanal a S/ 35")).isEqualTo("Panetón artesanal a S/ 35");
        // Un pedido a la IA sin cifra no se destaca.
        assertThat(PublicacionService.ofertaDestacada("Créame un anuncio para mi cafetería")).isNull();
        assertThat(PublicacionService.ofertaDestacada(null)).isNull();
    }
}
