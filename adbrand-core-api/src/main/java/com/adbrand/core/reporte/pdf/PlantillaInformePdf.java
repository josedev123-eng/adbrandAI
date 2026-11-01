package com.adbrand.core.reporte.pdf;

import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;

// HU 38: plantilla del informe de campañas en PDF.
// Usa el mismo InformeCampanasResponse que la pantalla de informes (HU 37), así los datos coinciden (criterio 2).
// Los textos (Instagram, En revisión...) son los mismos que muestra la web en reportesApi.js.
@Component
public class PlantillaInformePdf {

    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final Map<String, String> NOMBRES_RED = Map.of("INSTAGRAM", "Instagram", "FACEBOOK", "Facebook");
    static final Map<String, String> NOMBRES_ESTADO =
            Map.of("APROBADO", "Aprobado", "DUDOSO", "En revisión", "RECHAZADO", "Rechazado");

    private static final Color MARCA = new Color(0x4F, 0x46, 0xE5);
    private static final Color FONDO_CABECERA = new Color(0xEE, 0xF2, 0xFF);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, MARCA);
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    // Devuelve el PDF listo para descargar. "emitido" es la fecha que se imprime en el informe.
    public byte[] generar(InformeCampanasResponse informe, LocalDateTime emitido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(documento, salida);
        documento.addTitle("Informe de campañas - " + informe.empresa());
        documento.addCreator("AdBrand.AI");
        documento.open();

        documento.add(new Paragraph("Informe de campañas", TITULO));
        documento.add(new Paragraph("Empresa: " + informe.empresa(), SUBTITULO));
        documento.add(new Paragraph("Emitido el " + emitido.format(FORMATO_FECHA), PEQUENA));
        documento.add(espacio());

        documento.add(totales(informe));
        documento.add(espacio());

        if (informe.campanas().isEmpty()) {
            documento.add(new Paragraph("Todavía no hay campañas registradas.", NORMAL));
        } else {
            documento.add(tablaCampanas(informe));
        }

        documento.add(espacio());
        documento.add(new Paragraph("Generado por AdBrand.AI", PEQUENA));
        documento.close();
        return salida.toByteArray();
    }

    // Criterio 1: los totales, igual que el resumen de la pantalla.
    private static PdfPTable totales(InformeCampanasResponse informe) {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        for (String titulo : new String[] {"Campañas", "Aprobadas", "En revisión", "Rechazadas"}) {
            tabla.addCell(cabecera(titulo));
        }
        for (long valor : new long[] {informe.total(), informe.aprobados(), informe.dudosos(), informe.rechazados()}) {
            PdfPCell celda = celda(String.valueOf(valor), NEGRITA);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }
        return tabla;
    }

    // Criterio 1: la tabla de campañas, con las mismas columnas que la pantalla.
    private static PdfPTable tablaCampanas(InformeCampanasResponse informe) {
        PdfPTable tabla = new PdfPTable(new float[] {2.2f, 1.6f, 5f, 1.8f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : new String[] {"Fecha", "Red social", "Oferta", "Estado"}) {
            tabla.addCell(cabecera(titulo));
        }
        for (CampanaInformeResponse campana : informe.campanas()) {
            tabla.addCell(celda(campana.fecha() == null ? "" : campana.fecha().format(FORMATO_FECHA), NORMAL));
            tabla.addCell(celda(nombre(NOMBRES_RED, campana.redSocial()), NORMAL));
            tabla.addCell(celda(campana.oferta() == null ? "" : campana.oferta(), NORMAL));
            tabla.addCell(celda(nombre(NOMBRES_ESTADO, campana.estado()), NORMAL));
        }
        return tabla;
    }

    private static String nombre(Map<String, String> nombres, Enum<?> valor) {
        return valor == null ? "" : nombres.getOrDefault(valor.name(), valor.name());
    }

    private static PdfPCell cabecera(String texto) {
        PdfPCell celda = celda(texto, NEGRITA);
        celda.setBackgroundColor(FONDO_CABECERA);
        return celda;
    }

    private static PdfPCell celda(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setPadding(5);
        return celda;
    }

    private static Paragraph espacio() {
        return new Paragraph(" ", NORMAL);
    }
}
