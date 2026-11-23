package com.adbrand.core.brandkit.service;

import com.adbrand.core.brandkit.entity.KitMarca;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;

// Manual de marca en 1 página (PDF): logo, eslogan, colores con sus códigos, letras y cómo habla la marca.
// Es lo que la empresa lleva a la imprenta para sus tarjetas, volantes, stickers o letrero.
@Component
public class ManualMarcaPdf {

    private static final Color MARCA = new Color(0x4F, 0x46, 0xE5);
    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(0x1B, 0x1D, 0x2E));
    private static final Font ESLOGAN = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 13, MARCA);
    private static final Font SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, MARCA);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    public byte[] generar(KitMarca kit) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(documento, salida);
        documento.addTitle("Manual de marca - " + kit.getNombreNegocio());
        documento.addCreator("AdBrand.AI");
        documento.open();

        documento.add(new Paragraph("MANUAL DE MARCA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY)));
        documento.add(new Paragraph(kit.getNombreNegocio(), TITULO));
        if (tiene(kit.getEslogan())) {
            documento.add(new Paragraph("\"" + kit.getEslogan() + "\"", ESLOGAN));
        }
        documento.add(espacio());

        // Logo.
        documento.add(new Paragraph("Logo", SECCION));
        Image logo = logoDe(kit);
        if (logo != null) {
            logo.scaleToFit(170, 170);
            logo.setSpacingBefore(6);
            documento.add(logo);
            documento.add(new Paragraph("Úsalo sobre fondo claro y sin deformarlo. Deja aire alrededor.", PEQUENA));
        } else {
            documento.add(new Paragraph("Todavía no tienes un logo guardado en tu kit.", NORMAL));
        }
        documento.add(espacio());

        // Colores.
        List<String> colores = IdentidadVisualService.coloresDe(kit.getPaletaColores());
        if (!colores.isEmpty()) {
            documento.add(new Paragraph("Colores", SECCION));
            documento.add(tablaColores(colores));
            documento.add(new Paragraph("Pide estos códigos a tu imprenta para que tus colores salgan iguales.", PEQUENA));
            documento.add(espacio());
        }

        // Letras.
        if (tiene(kit.getTipografiaPrimaria())) {
            documento.add(new Paragraph("Letras", SECCION));
            documento.add(new Paragraph("Principal: " + kit.getTipografiaPrimaria(), NORMAL));
            if (tiene(kit.getTipografiaSecundaria())) {
                documento.add(new Paragraph("Secundaria: " + kit.getTipografiaSecundaria(), NORMAL));
            }
            documento.add(espacio());
        }

        // Cómo habla la marca y qué transmite.
        if (tiene(kit.getVozMarca()) || tiene(kit.getSentimiento()) || tiene(kit.getDiferencial())) {
            documento.add(new Paragraph("Cómo habla tu marca", SECCION));
            if (tiene(kit.getVozMarca())) {
                documento.add(new Paragraph(kit.getVozMarca(), NORMAL));
            }
            if (tiene(kit.getSentimiento())) {
                documento.add(new Paragraph(new Phrase("Lo que siente tu cliente: ", NEGRITA)));
                documento.add(new Paragraph(kit.getSentimiento(), NORMAL));
            }
            if (tiene(kit.getDiferencial())) {
                documento.add(new Paragraph(new Phrase("Lo que te hace diferente: ", NEGRITA)));
                documento.add(new Paragraph(kit.getDiferencial(), NORMAL));
            }
            documento.add(espacio());
        }

        documento.add(new Paragraph(
                "Antes de registrar tu logo como marca en INDECOPI, verifica que no se parezca a una marca ya existente.",
                PEQUENA));
        documento.add(new Paragraph("Hecho con AdBrand.AI", PEQUENA));
        documento.close();
        return salida.toByteArray();
    }

    private static PdfPTable tablaColores(List<String> colores) {
        PdfPTable tabla = new PdfPTable(colores.size());
        tabla.setWidthPercentage(Math.min(100, colores.size() * 20));
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.setSpacingBefore(6);
        for (String hex : colores) {
            PdfPCell muestra = new PdfPCell(new Phrase(" "));
            muestra.setFixedHeight(42);
            muestra.setBackgroundColor(Color.decode(hex));
            muestra.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(muestra);
        }
        for (String hex : colores) {
            PdfPCell codigo = new PdfPCell(new Phrase(hex, NEGRITA));
            codigo.setBorder(Rectangle.NO_BORDER);
            codigo.setPaddingTop(4);
            tabla.addCell(codigo);
        }
        return tabla;
    }

    private static Image logoDe(KitMarca kit) {
        if (!tiene(kit.getLogoImagen())) {
            return null;
        }
        try {
            return Image.getInstance(Base64.getDecoder().decode(kit.getLogoImagen()));
        } catch (Exception ex) {
            return null;
        }
    }

    private static Paragraph espacio() {
        Paragraph espacio = new Paragraph(" ");
        espacio.setSpacingAfter(2);
        return espacio;
    }

    private static boolean tiene(String texto) {
        return texto != null && !texto.isBlank();
    }
}
