package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.service.MarcaDelNegocio.Marca;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Ajuste 14 de la revisión: que cada opción parezca un anuncio de verdad y no letras encima de una foto.
// La IA hace solo la foto (sin texto, así nunca sale mal escrito) y aquí se arma el diseño, con 4 plantillas
// distintas (una por opción): titular grande, etiqueta con el precio o la promo, botón "Pide por WhatsApp",
// logo, nombre y colores del kit. Funciona en cualquier formato (cuadrado, vertical u horizontal).
public final class DisenoAnuncio {

    public enum Plantilla {
        // 1: franja de color arriba con el titular, la foto grande y el botón abajo.
        TITULAR,
        // 2: la foto a todo el fondo y un bloque de color redondeado con el texto abajo.
        BLOQUE,
        // 3: fondo claro, la foto en un marco redondeado y el texto en el color de la marca.
        MINIMAL,
        // 4: mitad de color con el texto y mitad foto.
        DIVIDIDO;

        // La opción 1 usa TITULAR, la 2 BLOQUE, la 3 MINIMAL y la 4 DIVIDIDO.
        public static Plantilla deOpcion(int numero) {
            Plantilla[] todas = values();
            return todas[Math.floorMod(numero - 1, todas.length)];
        }
    }

    // Lo que se escribe en el anuncio. etiqueta (precio o promo), llamado y condiciones pueden ser null.
    // condiciones: la letra chica de INDECOPI ("Válido hasta el 24/12/2026 o hasta agotar stock (50 unidades).").
    public record Textos(String titular, String etiqueta, String llamado, String condiciones) {

        public Textos(String titular, String etiqueta, String llamado) {
            this(titular, etiqueta, llamado, null);
        }

        public Textos conCondiciones(String nuevas) {
            return new Textos(titular, etiqueta, llamado, nuevas == null || nuevas.isBlank() ? null : nuevas.trim());
        }
    }

    // "2026-12-24" y "50" → "Válido hasta el 24/12/2026 o hasta agotar stock (50 unidades)." (null si no hay nada).
    public static String condicionesDe(String hasta, String stock) {
        StringBuilder texto = new StringBuilder();
        if (hasta != null && hasta.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] partes = hasta.split("-");
            texto.append("Válido hasta el ").append(partes[2]).append('/').append(partes[1]).append('/').append(partes[0]);
        }
        if (stock != null && stock.matches("\\d{1,6}") && Integer.parseInt(stock) > 0) {
            texto.append(texto.isEmpty() ? "Hasta agotar stock" : " o hasta agotar stock")
                    .append(" (").append(Integer.parseInt(stock)).append(" unidades)");
        }
        return texto.isEmpty() ? null : texto + ".";
    }

    private static final Pattern PRECIO = Pattern.compile("(?i)s/\\.?\\s*(\\d+(?:[.,]\\d{1,2})?)");
    private static final Pattern PROMO = Pattern.compile("(?i)\\b(\\d+\\s*[x×]\\s*\\d+)\\b");
    private static final Pattern DESCUENTO = Pattern.compile("(-?\\d{1,2})\\s*%");

    private DisenoAnuncio() {
    }

    // Saca de la oferta lo que va en el anuncio: "2 panetones por S/ 35" → titular + etiqueta "S/ 35".
    public static Textos textosDe(String oferta, String texto, boolean conWhatsapp) {
        return textosDe(null, oferta, texto, conWhatsapp);
    }

    // HU 67: con el titular corto de la IA (si lo hay) en vez de la primera frase del texto.
    public static Textos textosDe(String titularIa, String oferta, String texto, boolean conWhatsapp) {
        String titular = VideoAnuncioService.titularDe(titularIa, oferta, texto);
        String fuente = oferta == null ? "" : oferta;
        String etiqueta = null;
        Matcher promo = PROMO.matcher(fuente);
        Matcher precio = PRECIO.matcher(fuente);
        Matcher descuento = DESCUENTO.matcher(fuente);
        if (promo.find()) {
            etiqueta = promo.group(1).replaceAll("\\s+", "").replace('×', 'x').toUpperCase(Locale.ROOT);
        } else if (descuento.find()) {
            etiqueta = "-" + descuento.group(1).replace("-", "") + "%";
        } else if (precio.find()) {
            etiqueta = "S/ " + precio.group(1);
            // El precio ya va grande en la etiqueta: "2 panetones por S/ 35 hasta…" → "2 panetones hasta…".
            String sinPrecio = titular.replaceAll("(?i)\\s*(a solo|a sólo|por solo|por sólo|por|a|desde|x)?\\s*s/\\.?\\s*\\d+([.,]\\d{1,2})?", "")
                    .replaceAll("\\s{2,}", " ").trim();
            if (sinPrecio.length() >= 8) {
                titular = sinPrecio;
            }
        }
        return new Textos(titular, etiqueta, conWhatsapp ? "Pide por WhatsApp" : "¡Te esperamos!");
    }

    public static BufferedImage dibujar(BufferedImage foto, Textos textos, Marca marca, Plantilla plantilla,
                                        int ancho, int alto) {
        BufferedImage lienzo = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lienzo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        switch (plantilla) {
            case TITULAR -> titular(g, foto, textos, marca, ancho, alto);
            case BLOQUE -> bloque(g, foto, textos, marca, ancho, alto);
            case MINIMAL -> minimal(g, foto, textos, marca, ancho, alto);
            case DIVIDIDO -> dividido(g, foto, textos, marca, ancho, alto);
        }
        letraChica(g, textos.condiciones(), ancho, alto);
        g.dispose();
        return lienzo;
    }

    // ---------------------------------------------------------------- plantillas

    private static void titular(Graphics2D g, BufferedImage foto, Textos t, Marca m, int w, int h) {
        int u = Math.min(w, h);
        int margen = (int) (u * 0.06);
        int franja = (int) (h * (h > w ? 0.24 : 0.3));
        foto(g, foto, m, 0, franja, w, h - franja);
        // Franja de color con el titular.
        g.setPaint(new GradientPaint(0, 0, m.principal(), w, franja, m.principal().darker()));
        g.fillRect(0, 0, w, franja);
        g.setColor(m.acento());
        g.fillRect(0, franja - Math.max(4, u / 90), w, Math.max(4, u / 90));
        int anchoTexto = w - 2 * margen - (t.etiqueta() == null ? 0 : (int) (u * 0.2));
        texto(g, t.titular(), MarcaDelNegocio.textoSobre(m.principal()), margen, margen, anchoTexto, franja - 2 * margen,
                (int) (u * 0.085), 3);
        // Etiqueta redonda montada sobre el borde de la franja.
        if (t.etiqueta() != null) {
            int d = (int) (u * 0.22);
            etiqueta(g, t.etiqueta(), m, w - margen - d, franja - d / 2, d);
        }
        // Sombra abajo para el botón, el logo y el nombre.
        sombraAbajo(g, w, h, 0.25);
        int alturaBoton = (int) (u * 0.09);
        logoYNombre(g, m, margen, h - margen - alturaBoton, alturaBoton, Color.WHITE, w / 2 - margen);
        boton(g, t.llamado(), m.acento(), w - margen, h - margen - alturaBoton, alturaBoton, true);
    }

    private static void bloque(Graphics2D g, BufferedImage foto, Textos t, Marca m, int w, int h) {
        int u = Math.min(w, h);
        int margen = (int) (u * 0.06);
        foto(g, foto, m, 0, 0, w, h);
        sombraAbajo(g, w, h, 0.5);
        // Logo arriba a la izquierda.
        int lado = (int) (u * 0.13);
        logo(g, m, margen, margen, lado);
        if (t.etiqueta() != null) {
            int d = (int) (u * 0.21);
            etiqueta(g, t.etiqueta(), m, w - margen - d, margen, d);
        }
        // Bloque de color con el texto y el botón.
        int anchoBloque = (int) (w * (w > h ? 0.6 : 0.86));
        int altoBloque = (int) (u * 0.4);
        int x = margen;
        int y = h - margen - altoBloque;
        g.setColor(conAlfa(m.principal(), 235));
        g.fill(new RoundRectangle2D.Double(x, y, anchoBloque, altoBloque, u * 0.06, u * 0.06));
        g.setColor(m.acento());
        g.fill(new RoundRectangle2D.Double(x + u * 0.04, y + u * 0.035, u * 0.12, u * 0.012, u * 0.012, u * 0.012));
        int padding = (int) (u * 0.04);
        int alturaBoton = (int) (u * 0.085);
        texto(g, t.titular(), MarcaDelNegocio.textoSobre(m.principal()), x + padding, y + padding + (int) (u * 0.025),
                anchoBloque - 2 * padding, altoBloque - 2 * padding - alturaBoton - (int) (u * 0.045), (int) (u * 0.075), 3);
        boton(g, t.llamado(), m.acento(), x + padding, y + altoBloque - padding - alturaBoton, alturaBoton, false);
    }

    private static void minimal(Graphics2D g, BufferedImage foto, Textos t, Marca m, int w, int h) {
        int u = Math.min(w, h);
        int margen = (int) (u * 0.06);
        g.setColor(mezclar(m.principal(), Color.WHITE, 0.92));
        g.fillRect(0, 0, w, h);
        int altoFoto = (int) (h * (h > w ? 0.62 : 0.6));
        Shape marco = new RoundRectangle2D.Double(margen, margen, w - 2.0 * margen, altoFoto - margen, u * 0.06, u * 0.06);
        g.setClip(marco);
        foto(g, foto, m, margen, margen, w - 2 * margen, altoFoto - margen);
        g.setClip(null);
        if (t.etiqueta() != null) {
            int d = (int) (u * 0.2);
            etiqueta(g, t.etiqueta(), m, w - margen - d + (int) (u * 0.02), altoFoto - d + (int) (u * 0.02), d);
        }
        int yTexto = altoFoto + (int) (u * 0.045);
        int alturaBoton = (int) (u * 0.08);
        int altoTexto = h - yTexto - margen - alturaBoton - (int) (u * 0.04);
        texto(g, t.titular(), m.principal().darker(), margen, yTexto, w - 2 * margen, altoTexto, (int) (u * 0.072), 3);
        int yBoton = h - margen - alturaBoton;
        botonContorno(g, t.llamado(), m.principal(), margen, yBoton, alturaBoton);
        // Logo y nombre chicos abajo a la derecha.
        int lado = (int) (alturaBoton * 1.05);
        logo(g, m, w - margen - lado, yBoton - (lado - alturaBoton) / 2, lado);
        g.setColor(m.principal().darker());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (u * 0.032)));
        String nombre = recortar(m.nombre(), g.getFontMetrics(), (int) (w * 0.35));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(nombre, w - margen - lado - (int) (u * 0.02) - fm.stringWidth(nombre),
                yBoton + (alturaBoton + fm.getAscent() - fm.getDescent()) / 2);
    }

    private static void dividido(Graphics2D g, BufferedImage foto, Textos t, Marca m, int w, int h) {
        int u = Math.min(w, h);
        boolean vertical = h > w * 1.2;
        int margen = (int) (u * 0.06);
        if (vertical) {
            // En vertical (historias) la mitad de color va abajo.
            int corte = (int) (h * 0.55);
            foto(g, foto, m, 0, 0, w, corte);
            panelDividido(g, t, m, 0, corte, w, h - corte, u, margen);
        } else {
            int corte = (int) (w * 0.45);
            foto(g, foto, m, corte, 0, w - corte, h);
            panelDividido(g, t, m, 0, 0, corte, h, u, margen);
        }
    }

    private static void panelDividido(Graphics2D g, Textos t, Marca m, int x, int y, int w, int h, int u, int margen) {
        g.setPaint(new GradientPaint(x, y, m.principal(), x + w, y + h, m.principal().darker()));
        g.fillRect(x, y, w, h);
        // Rayas diagonales del color de acento, de adorno.
        g.setColor(conAlfa(m.acento(), 90));
        g.setStroke(new BasicStroke(u * 0.012f));
        for (int i = 0; i < 3; i++) {
            int desde = (int) (x + w - u * 0.12 + i * u * 0.035);
            g.drawLine(desde, y + h, desde + (int) (u * 0.12), y + h - (int) (u * 0.12));
        }
        int lado = (int) (u * 0.12);
        logo(g, m, x + margen, y + margen, lado);
        Color texto = MarcaDelNegocio.textoSobre(m.principal());
        int yTexto = y + margen + lado + (int) (u * 0.04);
        int alturaBoton = (int) (u * 0.08);
        int altoEtiqueta = t.etiqueta() == null ? 0 : (int) (u * 0.11);
        int altoTexto = y + h - margen - alturaBoton - altoEtiqueta - (int) (u * 0.06) - yTexto;
        int finTexto = texto(g, t.titular(), texto, x + margen, yTexto, w - 2 * margen, altoTexto, (int) (u * 0.08), 4);
        if (t.etiqueta() != null) {
            g.setColor(m.acento());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (u * 0.085)));
            g.drawString(t.etiqueta(), x + margen, finTexto + (int) (u * 0.1));
        }
        boton(g, t.llamado(), m.acento(), x + margen, y + h - margen - alturaBoton, alturaBoton, false);
    }

    // ---------------------------------------------------------------- piezas

    // HU 64: las condiciones de la promoción (INDECOPI) en una franja fina abajo, en el margen inferior que dejan
    // todas las plantillas: se leen bien pero no compiten con el titular.
    private static void letraChica(Graphics2D g, String texto, int w, int h) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        int u = Math.min(w, h);
        int alto = (int) (u * 0.045);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, h - alto, w, alto);
        int tamano = (int) (u * 0.024);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, tamano));
        while (g.getFontMetrics().stringWidth(texto) > w * 0.92 && tamano > 10) {
            tamano--;
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, tamano));
        }
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 235));
        g.drawString(texto, (w - fm.stringWidth(texto)) / 2, h - alto + (alto + fm.getAscent() - fm.getDescent()) / 2);
    }

    // La foto llenando el rectángulo sin deformarse; sin foto, un degradado con los colores de la marca.
    private static void foto(Graphics2D g, BufferedImage foto, Marca m, int x, int y, int w, int h) {
        if (foto == null) {
            g.setPaint(new GradientPaint(x, y, mezclar(m.principal(), Color.WHITE, 0.25), x + w, y + h, m.principal().darker()));
            g.fillRect(x, y, w, h);
            // Círculos suaves de adorno para que no quede plano.
            g.setColor(conAlfa(m.acento(), 70));
            int d = (int) (Math.min(w, h) * 0.7);
            g.fill(new Ellipse2D.Double(x + w - d * 0.7, y + h - d * 0.75, d, d));
            g.setColor(conAlfa(Color.WHITE, 40));
            g.fill(new Ellipse2D.Double(x - d * 0.3, y - d * 0.35, d * 0.8, d * 0.8));
            return;
        }
        Shape antes = g.getClip();
        g.clipRect(x, y, w, h);
        double escala = Math.max(w / (double) foto.getWidth(), h / (double) foto.getHeight());
        int fw = (int) Math.ceil(foto.getWidth() * escala);
        int fh = (int) Math.ceil(foto.getHeight() * escala);
        g.drawImage(foto, x + (w - fw) / 2, y + (h - fh) / 2, fw, fh, null);
        g.setClip(antes);
    }

    private static void sombraAbajo(Graphics2D g, int w, int h, double parte) {
        int desde = (int) (h * (1 - parte));
        g.setPaint(new GradientPaint(0, desde, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 170)));
        g.fillRect(0, desde, w, h - desde);
    }

    // Escribe el texto en líneas dentro de la caja, achicando la letra hasta que entre. Devuelve dónde terminó.
    private static int texto(Graphics2D g, String texto, Color color, int x, int y, int ancho, int alto,
                             int tamanoMaximo, int maxLineas) {
        if (texto == null || texto.isBlank() || alto <= 0) {
            return y;
        }
        int tamano = tamanoMaximo;
        List<String> lineas;
        while (true) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamano));
            lineas = partir(texto, g.getFontMetrics(), ancho);
            int altura = (int) (lineas.size() * tamano * 1.12);
            if ((lineas.size() <= maxLineas && altura <= alto) || tamano <= 18) {
                break;
            }
            tamano -= 2;
        }
        if (lineas.size() > maxLineas) {
            lineas = new ArrayList<>(lineas.subList(0, maxLineas));
            lineas.set(maxLineas - 1, recortar(lineas.get(maxLineas - 1) + "…", g.getFontMetrics(), ancho));
        }
        g.setColor(color);
        int base = y + g.getFontMetrics().getAscent();
        int salto = (int) (tamano * 1.12);
        for (String linea : lineas) {
            g.drawString(linea, x, base);
            base += salto;
        }
        return base - salto + g.getFontMetrics().getDescent();
    }

    // Etiqueta redonda con el precio o la promo (estilo "sticker"), en el color de acento.
    private static void etiqueta(Graphics2D g, String texto, Marca m, int x, int y, int d) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x + d * 0.04, y + d * 0.05, d, d));
        g.setColor(m.acento());
        g.fill(new Ellipse2D.Double(x, y, d, d));
        g.setColor(conAlfa(Color.WHITE, 200));
        g.setStroke(new BasicStroke(Math.max(2f, d * 0.025f)));
        g.draw(new Ellipse2D.Double(x + d * 0.07, y + d * 0.07, d * 0.86, d * 0.86));
        Color letra = MarcaDelNegocio.textoSobre(m.acento());
        int tamano = (int) (d * 0.34);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamano));
        while (g.getFontMetrics().stringWidth(texto) > d * 0.78 && tamano > 10) {
            tamano -= 2;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamano));
        }
        FontMetrics fm = g.getFontMetrics();
        g.setColor(letra);
        g.drawString(texto, x + (d - fm.stringWidth(texto)) / 2, y + (d + fm.getAscent() - fm.getDescent()) / 2);
    }

    // Botón con forma de píldora. "alDerecha": x es el borde derecho (el botón crece hacia la izquierda).
    private static void boton(Graphics2D g, String texto, Color fondo, int x, int y, int alto, boolean alDerecha) {
        if (texto == null) {
            return;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (alto * 0.42)));
        FontMetrics fm = g.getFontMetrics();
        int ancho = fm.stringWidth(texto) + (int) (alto * 1.45);
        int inicio = alDerecha ? x - ancho : x;
        g.setColor(fondo);
        g.fill(new RoundRectangle2D.Double(inicio, y, ancho, alto, alto, alto));
        g.setColor(MarcaDelNegocio.textoSobre(fondo));
        g.drawString(texto, inicio + alto / 2, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
        // Flechita al final.
        int f = (int) (alto * 0.16);
        int fx = inicio + ancho - alto / 2 + f / 2;
        int fy = y + alto / 2;
        Path2D flecha = new Path2D.Double();
        flecha.moveTo(fx - f, fy - f);
        flecha.lineTo(fx, fy);
        flecha.lineTo(fx - f, fy + f);
        g.setStroke(new BasicStroke(Math.max(2f, alto * 0.06f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(flecha);
    }

    private static void botonContorno(Graphics2D g, String texto, Color color, int x, int y, int alto) {
        if (texto == null) {
            return;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (alto * 0.42)));
        FontMetrics fm = g.getFontMetrics();
        int ancho = fm.stringWidth(texto) + alto;
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y, ancho, alto, alto, alto));
        g.setColor(MarcaDelNegocio.textoSobre(color));
        g.drawString(texto, x + alto / 2, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
    }

    // Logo del kit en un círculo blanco (o las iniciales en el color de la marca).
    private static void logo(Graphics2D g, Marca m, int x, int y, int lado) {
        int borde = Math.max(2, lado / 16);
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(new Ellipse2D.Double(x - borde + lado * 0.03, y - borde + lado * 0.05, lado + 2.0 * borde, lado + 2.0 * borde));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - borde, y - borde, lado + 2.0 * borde, lado + 2.0 * borde));
        if (m.logo() != null) {
            Shape antes = g.getClip();
            g.clip(new Ellipse2D.Double(x, y, lado, lado));
            double escala = Math.max(lado / (double) m.logo().getWidth(), lado / (double) m.logo().getHeight());
            int lw = (int) Math.ceil(m.logo().getWidth() * escala);
            int lh = (int) Math.ceil(m.logo().getHeight() * escala);
            g.drawImage(m.logo(), x + (lado - lw) / 2, y + (lado - lh) / 2, lw, lh, null);
            g.setClip(antes);
        } else {
            g.setColor(m.principal());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (lado * 0.42)));
            FontMetrics fm = g.getFontMetrics();
            String iniciales = m.iniciales();
            g.drawString(iniciales, x + (lado - fm.stringWidth(iniciales)) / 2, y + (lado + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private static void logoYNombre(Graphics2D g, Marca m, int x, int y, int alto, Color color, int anchoMaximo) {
        logo(g, m, x, y, alto);
        g.setColor(color);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (alto * 0.36)));
        FontMetrics fm = g.getFontMetrics();
        String nombre = recortar(m.nombre(), fm, anchoMaximo - alto - alto / 4);
        g.drawString(nombre, x + alto + alto / 4, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
    }

    // ---------------------------------------------------------------- ayudas

    private static List<String> partir(String texto, FontMetrics medidas, int anchoMaximo) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.trim().split("\\s+")) {
            String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (medidas.stringWidth(prueba) > anchoMaximo && !actual.isEmpty()) {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static String recortar(String texto, FontMetrics medidas, int anchoMaximo) {
        String valor = texto == null ? "" : texto;
        if (medidas.stringWidth(valor) <= anchoMaximo) {
            return valor;
        }
        while (valor.length() > 1 && medidas.stringWidth(valor + "…") > anchoMaximo) {
            valor = valor.substring(0, valor.length() - 1);
        }
        return valor.trim() + "…";
    }

    private static Color conAlfa(Color color, int alfa) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alfa);
    }

    // "cuanto" = 0 → el primer color; 1 → el segundo.
    private static Color mezclar(Color a, Color b, double cuanto) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * cuanto),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * cuanto),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * cuanto));
    }
}
