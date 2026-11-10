package com.adbrand.core.reporte.word;

import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

// HU 39: arma el informe de campañas como documento Word (.docx) editable.
// Usa el mismo InformeCampanasResponse que la pantalla y el PDF, así los tres muestran los mismos datos.
@Component
public class PlantillaInformeWord {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String, String> REDES = Map.of("INSTAGRAM", "Instagram", "FACEBOOK", "Facebook");
    private static final Map<String, String> ESTADOS = Map.of(
            "APROBADO", "Aprobado", "DUDOSO", "En revisión", "RECHAZADO", "Rechazado");

    // Devuelve el .docx listo para descargar. "emitido" es la fecha que se imprime en el informe.
    public byte[] generar(InformeCampanasResponse informe, LocalDateTime emitido) {
        try (XWPFDocument documento = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            documento.createParagraph().createRun().setText("Informe de campañas");
            documento.createParagraph().createRun().setText("Empresa: " + informe.empresa());
            documento.createParagraph().createRun().setText("Emitido: " + emitido.format(FECHA));

            if (informe.campanas().isEmpty()) {
                documento.createParagraph().createRun().setText("No hay campañas para mostrar.");
            } else {
                totales(documento, informe);
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
        llenar(tabla.getRow(0), "Campañas", "Aprobadas", "En revisión", "Rechazadas");
        llenar(tabla.getRow(1), String.valueOf(informe.total()), String.valueOf(informe.aprobados()),
                String.valueOf(informe.dudosos()), String.valueOf(informe.rechazados()));
    }

    // Una fila por anuncio, en el mismo orden que la pantalla (del más reciente al más antiguo).
    private void detalle(XWPFDocument documento, InformeCampanasResponse informe) {
        XWPFTable tabla = documento.createTable(1, 4);
        llenar(tabla.getRow(0), "Fecha", "Red social", "Oferta", "Estado");
        for (CampanaInformeResponse campana : informe.campanas()) {
            llenar(tabla.createRow(),
                    campana.fecha() == null ? "" : campana.fecha().format(FECHA),
                    REDES.getOrDefault(campana.redSocial().name(), campana.redSocial().name()),
                    campana.oferta(),
                    ESTADOS.getOrDefault(campana.estado().name(), campana.estado().name()));
        }
    }

    private static void llenar(XWPFTableRow fila, String... textos) {
        for (int i = 0; i < textos.length; i++) {
            fila.getCell(i).setText(textos[i]);
        }
    }
}
