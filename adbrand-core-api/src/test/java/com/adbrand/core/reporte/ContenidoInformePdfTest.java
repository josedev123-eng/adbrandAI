package com.adbrand.core.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.pdf.PlantillaInformePdf;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// HU 38: pruebas del contenido del PDF, página por página, sin base de datos.
class ContenidoInformePdfTest {

    private static final LocalDateTime EMITIDO = LocalDateTime.of(2026, 11, 3, 9, 30);
    private final PlantillaInformePdf plantilla = new PlantillaInformePdf();

    // Al leer el PDF, las celdas de una fila salen pegadas y las ofertas largas se cortan en varias líneas.
    // Por eso se compara sin espacios ni saltos de línea: lo que importa es el contenido y su orden.
    static String compacto(String texto) {
        return texto.replaceAll("\\s+", "");
    }

    private static CampanaInformeResponse campana(long id, String oferta, RedSocial red, EstadoContenido estado) {
        return new CampanaInformeResponse(id, LocalDateTime.of(2026, 11, 1, 10, 0).plusHours(id), red, oferta, estado);
    }

    private String textoDe(InformeCampanasResponse informe) throws Exception {
        return compacto(PlantillaInformePdfTest.texto(plantilla.generar(informe, EMITIDO)));
    }

    @Test
    void losTotalesVanDebajoDeSuTitulo() throws Exception {
        InformeCampanasResponse informe = new InformeCampanasResponse("Bodega Lucho", 7, 4, 2, 1, List.of(
                campana(1, "Promo", RedSocial.INSTAGRAM, EstadoContenido.APROBADO)));

        assertThat(textoDe(informe)).contains(compacto("Campañas Aprobadas En revisión Rechazadas 7 4 2 1"));
    }

    @Test
    void cadaFilaLlevaFechaRedOfertaYEstadoEnOrden() throws Exception {
        InformeCampanasResponse informe = new InformeCampanasResponse("Bodega Lucho", 3, 1, 1, 1, List.of(
                campana(3, "Té milagroso", RedSocial.INSTAGRAM, EstadoContenido.DUDOSO),
                campana(2, "Cigarrillos importados", RedSocial.FACEBOOK, EstadoContenido.RECHAZADO),
                campana(1, "2 panetones por S/ 35", RedSocial.INSTAGRAM, EstadoContenido.APROBADO)));

        assertThat(textoDe(informe)).contains(compacto("Fecha Red social Oferta Estado "
                + "01/11/2026 13:00 Instagram Té milagroso En revisión "
                + "01/11/2026 12:00 Facebook Cigarrillos importados Rechazado "
                + "01/11/2026 11:00 Instagram 2 panetones por S/ 35 Aprobado"));
    }

    @Test
    void respetaTildesEnesYSimbolos() throws Exception {
        InformeCampanasResponse informe = new InformeCampanasResponse("Pollería El Ñandú & Cía", 1, 1, 0, 0, List.of(
                campana(1, "Combo <familiar> a S/ 49.90 – ¡solo hoy! 50% dscto.", RedSocial.FACEBOOK,
                        EstadoContenido.APROBADO)));

        assertThat(textoDe(informe))
                .contains(compacto("Empresa: Pollería El Ñandú & Cía"))
                .contains(compacto("Combo <familiar> a S/ 49.90 – ¡solo hoy! 50% dscto. Aprobado"));
    }

    @Test
    void conMuchasCampanasUsaVariasPaginasYRepiteLosTitulosDeLaTabla() throws Exception {
        List<CampanaInformeResponse> campanas = new ArrayList<>();
        for (long id = 80; id >= 1; id--) {
            campanas.add(campana(id, "Oferta número " + id, RedSocial.INSTAGRAM, EstadoContenido.APROBADO));
        }
        byte[] pdf = plantilla.generar(new InformeCampanasResponse("Bodega Lucho", 80, 80, 0, 0, campanas), EMITIDO);

        PdfReader lector = new PdfReader(pdf);
        int paginas = lector.getNumberOfPages();
        assertThat(paginas).isGreaterThan(1);
        for (int pagina = 1; pagina <= paginas; pagina++) {
            assertThat(new PdfTextExtractor(lector).getTextFromPage(pagina)).contains("Red social");
        }
        lector.close();

        String texto = compacto(PlantillaInformePdfTest.texto(pdf));
        for (int id = 1; id <= 80; id++) {
            assertThat(texto).contains(compacto("Instagram Oferta número " + id + " Aprobado"));
        }
    }

    @Test
    void unaCampanaSinDatosNoRompeElPdf() throws Exception {
        InformeCampanasResponse informe = new InformeCampanasResponse("Bodega Lucho", 1, 0, 0, 0, List.of(
                new CampanaInformeResponse(1L, null, null, null, null)));

        assertThat(textoDe(informe)).contains("Fecha").doesNotContain("null");
    }
}
