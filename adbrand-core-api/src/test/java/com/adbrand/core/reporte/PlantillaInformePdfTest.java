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
import java.util.List;
import org.junit.jupiter.api.Test;

// HU 38: la plantilla arma un PDF válido con la empresa, los totales y la tabla. Sin base de datos.
class PlantillaInformePdfTest {

    private static final LocalDateTime EMITIDO = LocalDateTime.of(2026, 11, 3, 9, 30);
    private final PlantillaInformePdf plantilla = new PlantillaInformePdf();

    // Lee el texto de todas las páginas del PDF, como lo vería quien lo abre.
    static String texto(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina)).append('\n');
        }
        lector.close();
        return texto.toString();
    }

    @Test
    void armaUnPdfConLaEmpresaLosTotalesYLasCampanas() throws Exception {
        InformeCampanasResponse informe = new InformeCampanasResponse("Panadería Doña Rosa", 2, 1, 1, 0, List.of(
                new CampanaInformeResponse(2L, LocalDateTime.of(2026, 11, 2, 18, 5), RedSocial.FACEBOOK,
                        "Martes de pan con chicharrón", EstadoContenido.DUDOSO),
                new CampanaInformeResponse(1L, LocalDateTime.of(2026, 11, 1, 10, 0), RedSocial.INSTAGRAM,
                        "2 panetones por S/ 35", EstadoContenido.APROBADO)));

        byte[] pdf = plantilla.generar(informe, EMITIDO);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(texto(pdf))
                .contains("Informe de campañas")
                .contains("Empresa: Panadería Doña Rosa")
                .contains("Emitido el 03/11/2026 09:30")
                .contains("Martes de pan con chicharrón")
                .contains("En revisión")
                .contains("02/11/2026 18:05");
    }

    @Test
    void sinCampanasLoDiceEnElPdf() throws Exception {
        byte[] pdf = plantilla.generar(new InformeCampanasResponse("Bodega Lucho", 0, 0, 0, 0, List.of()), EMITIDO);

        assertThat(texto(pdf)).contains("Empresa: Bodega Lucho").contains("Todavía no hay campañas registradas.");
    }
}
