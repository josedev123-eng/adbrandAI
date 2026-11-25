package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.dto.ObservacionLegal;
import com.adbrand.core.contenido.service.RevisionLegalOferta;
import java.util.List;
import org.junit.jupiter.api.Test;

// HU 64: "Tu oferta, sin multas". Reglas básicas de publicidad de promociones (Ley 29571).
class RevisionLegalOfertaTest {

    private static List<String> codigos(String oferta, String texto) {
        return RevisionLegalOferta.revisar(oferta, texto).stream().map(ObservacionLegal::codigo).toList();
    }

    @Test
    void unaPromocionSinFechaNiStockRecibeLasDosObservaciones() {
        assertThat(codigos("2x1 en panetones", "¡Llévate 2 panetones por solo S/ 35!"))
                .containsExactly("FALTA_VIGENCIA", "FALTA_STOCK");
    }

    @Test
    void conVigenciaYStockNoHayObservaciones() {
        assertThat(codigos("2x1 en panetones",
                "¡2x1 en panetones! Válido hasta el 24/12 o hasta agotar stock (50 unidades).")).isEmpty();
    }

    @Test
    void unAnuncioQueNoEsPromocionNoPideFechaNiStock() {
        assertThat(codigos("Nuestro pan de cada día", "Pan recién horneado a las 6 am, como en casa.")).isEmpty();
    }

    @Test
    void avisaDeAfirmacionesQueHayQueProbar() {
        assertThat(codigos("Café", "El mejor café de Lima, recién molido.")).contains("AFIRMACION_SIN_RESPALDO");
    }

    @Test
    void avisaSiElPrecioNoIncluyeElIgv() {
        assertThat(codigos("Combo", "Combo familiar a S/ 50 + IGV. Solo hoy, stock limitado."))
                .containsExactly("PRECIO_SIN_IMPUESTOS");
    }
}
