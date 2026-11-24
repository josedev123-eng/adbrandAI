package com.adbrand.core.imagen.service;

import com.adbrand.core.imagen.dto.ImagenGenerada;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;

// Imagen de prueba: un SVG con un fondo de color y la descripción escrita encima.
// No llama a ninguna IA ni gasta crédito. Se usa con ia.imagen.mock=true, en las pruebas
// y como último respaldo si las IA reales fallan.
@Component
public class GeneradorImagenSimulado implements GeneradorImagen {

    public static final String NOMBRE = "simulado";

    // Pares de colores (fondo, acento) parecidos a los de la web.
    private static final String[][] COLORES = {
            {"#4F46E5", "#B5179E"}, {"#0F766E", "#F59E0B"}, {"#C2410C", "#FDE68A"},
            {"#1D4ED8", "#38BDF8"}, {"#9D174D", "#FBCFE8"}, {"#166534", "#BBF7D0"},
    };
    private static final int LETRAS_POR_LINEA = 28;
    private static final int MAXIMO_LINEAS = 6;

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ImagenGenerada generar(String descripcion, ImagenGenerada imagenBase) {
        String texto = descripcion == null || descripcion.isBlank() ? "Imagen de prueba" : descripcion.strip();
        // El mismo texto da siempre el mismo color; un retoque cambia de color para que se note la nueva versión.
        int indice = Math.floorMod(texto.hashCode() + (imagenBase == null ? 0 : 1), COLORES.length);
        String svg = dibujar(texto, COLORES[indice][0], COLORES[indice][1], imagenBase != null);
        String base64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new ImagenGenerada("image/svg+xml", base64, NOMBRE, true);
    }

    private String dibujar(String texto, String fondo, String acento, boolean esRetoque) {
        StringBuilder lineas = new StringBuilder();
        List<String> partes = partirEnLineas(texto);
        int y = 512 - (partes.size() - 1) * 26;
        for (String linea : partes) {
            lineas.append("<text x=\"512\" y=\"").append(y)
                    .append("\" text-anchor=\"middle\" font-family=\"Segoe UI, Arial, sans-serif\" font-size=\"44\" ")
                    .append("font-weight=\"700\" fill=\"#FFFFFF\">").append(escapar(linea)).append("</text>");
            y += 52;
        }
        String etiqueta = esRetoque ? "IMAGEN DE PRUEBA · RETOQUE" : "IMAGEN DE PRUEBA";
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">\
                <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">\
                <stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/></linearGradient></defs>\
                <rect width="1024" height="1024" fill="url(#g)"/>\
                <circle cx="860" cy="170" r="120" fill="#FFFFFF" fill-opacity="0.12"/>\
                <circle cx="150" cy="880" r="180" fill="#FFFFFF" fill-opacity="0.08"/>\
                <text x="512" y="120" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="26" \
                letter-spacing="6" fill="#FFFFFF" fill-opacity="0.8">%s</text>%s</svg>"""
                .formatted(fondo, acento, etiqueta, lineas);
    }

    // Parte el texto en líneas cortas para que quepa en la imagen.
    static List<String> partirEnLineas(String texto) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            if (!actual.isEmpty() && actual.length() + 1 + palabra.length() > LETRAS_POR_LINEA) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }
            if (!actual.isEmpty()) {
                actual.append(' ');
            }
            actual.append(palabra);
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        if (lineas.size() > MAXIMO_LINEAS) {
            List<String> recortadas = new ArrayList<>(lineas.subList(0, MAXIMO_LINEAS));
            recortadas.set(MAXIMO_LINEAS - 1, recortadas.get(MAXIMO_LINEAS - 1) + "…");
            return recortadas;
        }
        return lineas;
    }

    // Evita que un texto con < o & rompa el SVG.
    static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
