package com.adbrand.core.reporte.word;

import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

// HU 39: arma el informe de campañas como documento Word (.docx) editable, con formato de reporte ejecutivo.
// Usa el mismo InformeCampanasResponse que la pantalla y el PDF, así los tres muestran los mismos datos.
@Component
public class PlantillaInformeWord {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String, String> REDES = Map.of("INSTAGRAM", "Instagram", "FACEBOOK", "Facebook");
    private static final Map<String, String> ESTADOS = Map.of(
            "APROBADO", "Aprobado", "DUDOSO", "En revisión", "RECHAZADO", "Rechazado");

    // Los mismos colores que la web (adbrand-web/src/index.css), sin el "#".
    private static final String MORADO = "4F46E5";
    private static final String MORADO_SUAVE = "ECEBFD";
    private static final String GRIS = "6B7280";
    private static final String FUENTE = "Calibri";

    public byte[] generar(InformeCampanasResponse informe, LocalDateTime emitido) {
        return generar(informe, emitido, null);
    }

    // Devuelve el .docx listo para descargar. "emitido" es la fecha que se imprime en el informe.
    // "filtro" describe el periodo y la campaña elegidos (HU 40); sin filtros va en null.
    public byte[] generar(InformeCampanasResponse informe, LocalDateTime emitido, String filtro) {
        try (XWPFDocument documento = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            texto(documento, "Informe de campañas", 22, true, MORADO);
            texto(documento, "Empresa: " + informe.empresa(), 13, true, null);
            texto(documento, "Emitido: " + emitido.format(FECHA), 10, false, GRIS);
            if (filtro != null) {
                texto(documento, filtro, 10, false, GRIS);
            }

            if (informe.campanas().isEmpty()) {
                texto(documento, "No hay campañas para mostrar.", 11, false, null);
            } else {
                texto(documento, "Resumen", 14, true, MORADO);
                totales(documento, informe);
                texto(documento, "Detalle de campañas", 14, true, MORADO);
                detalle(documento, informe);
            }

            documento.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el informe en Word.", ex);
        }
    }

    // Los mismos totales que la pantalla: campañas, aprobadas, en revisión y rechazadas.
    private void totales(XWPFDocument documento, InformeCampanasResponse informe) {
        XWPFTable tabla = documento.createTable(2, 4);
        tabla.setWidth("100%");
        encabezado(tabla.getRow(0), "Campañas", "Aprobadas", "En revisión", "Rechazadas");
        llenar(tabla.getRow(1), String.valueOf(informe.total()), String.valueOf(informe.aprobados()),
                String.valueOf(informe.dudosos()), String.valueOf(informe.rechazados()));
        for (XWPFTableCell celda : tabla.getRow(1).getTableCells()) {
            celda.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        }
    }

    // Una fila por anuncio, en el mismo orden que la pantalla (del más reciente al más antiguo).
    private void detalle(XWPFDocument documento, InformeCampanasResponse informe) {
        XWPFTable tabla = documento.createTable(1, 4);
        tabla.setWidth("100%");
        encabezado(tabla.getRow(0), "Fecha", "Red social", "Oferta", "Estado");
        for (CampanaInformeResponse campana : informe.campanas()) {
            llenar(tabla.createRow(),
                    campana.fecha() == null ? "" : campana.fecha().format(FECHA),
                    REDES.getOrDefault(campana.redSocial().name(), campana.redSocial().name()),
                    campana.oferta(),
                    ESTADOS.getOrDefault(campana.estado().name(), campana.estado().name()));
        }
    }

    // Un párrafo con un solo estilo. color va sin "#"; null deja el negro de Word.
    private static void texto(XWPFDocument documento, String contenido, int tamano, boolean negrita, String color) {
        XWPFParagraph parrafo = documento.createParagraph();
        parrafo.setSpacingAfter(120);
        XWPFRun run = parrafo.createRun();
        run.setText(contenido);
        run.setFontFamily(FUENTE);
        run.setFontSize(tamano);
        run.setBold(negrita);
        if (color != null) {
            run.setColor(color);
        }
    }

    // Fila de títulos de una tabla: negrita, en morado y con fondo morado suave.
    private static void encabezado(XWPFTableRow fila, String... textos) {
        for (int i = 0; i < textos.length; i++) {
            XWPFTableCell celda = fila.getCell(i);
            celda.setColor(MORADO_SUAVE);
            XWPFRun run = celda.getParagraphs().get(0).createRun();
            run.setText(textos[i]);
            run.setFontFamily(FUENTE);
            run.setBold(true);
            run.setColor(MORADO);
        }
    }

    private static void llenar(XWPFTableRow fila, String... textos) {
        for (int i = 0; i < textos.length; i++) {
            XWPFRun run = fila.getCell(i).getParagraphs().get(0).createRun();
            run.setText(textos[i]);
            run.setFontFamily(FUENTE);
        }
    }
}
