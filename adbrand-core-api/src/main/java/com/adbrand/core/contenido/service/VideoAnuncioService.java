package com.adbrand.core.contenido.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 65: video corto del anuncio (8 s, vertical 720 x 1280, MP4) listo para Reels, TikTok o Estados de WhatsApp.
// Se arma con una plantilla, sin IA de video (gratis): la imagen del anuncio con un zoom suave y el texto que entra
// animado, y al final la tarjeta de la marca (logo, nombre, eslogan y "Pide por WhatsApp"). Sin música.
@Service
public class VideoAnuncioService {

    static final int ANCHO = 720;
    static final int ALTO = 1280;
    static final int FPS = 24;
    static final double SEGUNDOS_IMAGEN = 5.0;
    static final double SEGUNDOS_TOTAL = 8.0;

    private final ContenidoRepository contenidos;
    private final PrevisualizacionService previsualizacion;
    private final MarcaDelNegocio marcas;
    private final UsuarioRepository usuarios;
    private final LimiteConsumoIa limites;

    public VideoAnuncioService(ContenidoRepository contenidos, PrevisualizacionService previsualizacion,
                               MarcaDelNegocio marcas, UsuarioRepository usuarios, LimiteConsumoIa limites) {
        this.contenidos = contenidos;
        this.previsualizacion = previsualizacion;
        this.marcas = marcas;
        this.usuarios = usuarios;
        this.limites = limites;
    }

    // El video para Reels y Estados es de los planes de pago: con el plan gratis responde 403 PLAN_REQUERIDO.
    public boolean disponiblePara(Long usuarioId) {
        return !LimiteConsumoIa.GRATUITO.equals(limites.consumoDelMes(usuarioId).origen());
    }

    // Lo que se dibuja en el video. Separado para poder probar la plantilla sin base de datos.
    public record Guion(BufferedImage imagen, String frase, String nombre, String eslogan, BufferedImage logo,
                        Color colorMarca, String whatsapp) {
    }

    @Transactional(readOnly = true)
    public byte[] generar(Long usuarioId, Long contenidoId) {
        if (!disponiblePara(usuarioId)) {
            throw new AccesoDenegadoException("PLAN_REQUERIDO",
                    "El video para Reels y Estados está en los planes de pago. Elige un plan para usarlo.");
        }
        return codificar(guionDe(usuarioId, contenidoId));
    }

    // Lo que lleva el video de un anuncio: su foto actual (con los retoques), el titular y la marca.
    // HU 66: el video con IA usa el mismo guion para el titular y el cierre de marca.
    @Transactional(readOnly = true)
    public Guion guionDe(Long usuarioId, Long contenidoId) {
        Contenido contenido = contenidos.findById(contenidoId)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("ANUNCIO_NO_ENCONTRADO", "Ese anuncio no existe."));
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        // HU 61: el mismo logo, color y eslogan del kit que la imagen final y el afiche.
        MarcaDelNegocio.Marca marca = marcas.de(usuarioId);
        String telefono = usuario == null ? null : PublicacionService.celularWhatsapp(usuario.getTelefono());
        Guion guion = new Guion(
                MarcaDelNegocio.leerDataUrl(previsualizacion.imagen(usuarioId, contenidoId).imagen()),
                titularDe(contenido),
                marca.nombre(),
                marca.eslogan(),
                marca.logo(),
                marca.principal(),
                telefono == null ? null : "+" + telefono.substring(0, 2) + " " + telefono.substring(2));
        return guion;
    }

    // Arma los 192 cuadros (8 s a 24 por segundo) y los guarda como MP4.
    public static byte[] codificar(Guion guion) {
        File archivo = null;
        try {
            archivo = File.createTempFile("adbrand-video-", ".mp4");
            AWTSequenceEncoder codificador = AWTSequenceEncoder.createSequenceEncoder(archivo, FPS);
            int cuadros = (int) (SEGUNDOS_TOTAL * FPS);
            for (int i = 0; i < cuadros; i++) {
                codificador.encodeImage(cuadro(guion, i / (double) FPS));
            }
            codificador.finish();
            return Files.readAllBytes(archivo.toPath());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo crear el video del anuncio.", ex);
        } finally {
            if (archivo != null) {
                archivo.delete();
            }
        }
    }

    // Un cuadro del video en el segundo "t".
    public static BufferedImage cuadro(Guion guion, double t) {
        BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = lienzo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (t < SEGUNDOS_IMAGEN) {
            escenaAnuncio(g, guion, t);
        } else {
            escenaMarca(g, guion, t - SEGUNDOS_IMAGEN);
        }
        g.dispose();
        return lienzo;
    }

    // HU 66: el cierre de marca (logo, nombre, eslogan y "Pide por WhatsApp") en el segundo "t" desde que empieza.
    // Lo usa el video con IA para terminar igual que el video de plantilla.
    public static BufferedImage cuadroMarca(Guion guion, double t) {
        BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = lienzo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        escenaMarca(g, guion, t);
        g.dispose();
        return lienzo;
    }

    // Escena 1: la imagen con zoom suave (1.0 → 1.12) y la frase que sube y aparece desde el segundo 0.5.
    private static void escenaAnuncio(Graphics2D g, Guion guion, double t) {
        if (guion.imagen() != null) {
            double zoom = 1.0 + 0.12 * (t / SEGUNDOS_IMAGEN);
            BufferedImage img = guion.imagen();
            double escala = Math.max(ANCHO / (double) img.getWidth(), ALTO / (double) img.getHeight()) * zoom;
            int w = (int) (img.getWidth() * escala);
            int h = (int) (img.getHeight() * escala);
            g.drawImage(img, (ANCHO - w) / 2, (ALTO - h) / 2, w, h, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, guion.colorMarca(), ANCHO, ALTO, guion.colorMarca().darker()));
            g.fillRect(0, 0, ANCHO, ALTO);
        }
        // Sombra abajo para que el texto se lea sobre cualquier foto.
        g.setPaint(new GradientPaint(0, ALTO * 0.45f, new Color(0, 0, 0, 0), 0, ALTO, new Color(0, 0, 0, 200)));
        g.fillRect(0, (int) (ALTO * 0.45), ANCHO, (int) (ALTO * 0.55));

        double aparicion = Math.min(1.0, Math.max(0.0, (t - 0.5) / 0.8));
        if (aparicion > 0 && guion.frase() != null) {
            int desplazamiento = (int) ((1 - aparicion) * 60);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) aparicion));
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
            List<String> lineas = partir(guion.frase(), g.getFontMetrics(), ANCHO - 100);
            int y = ALTO - 180 - (lineas.size() - 1) * 64 + desplazamiento;
            for (String linea : lineas) {
                g.drawString(linea, 50, y);
                y += 64;
            }
            // Rayita del color de la marca debajo del texto.
            g.setColor(guion.colorMarca());
            g.fillRoundRect(50, y - 30, 120, 10, 10, 10);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    // Escena 2: la marca. Fondo del color principal, logo (o iniciales), nombre, eslogan y WhatsApp, con un fundido.
    private static void escenaMarca(Graphics2D g, Guion guion, double t) {
        g.setPaint(new GradientPaint(0, 0, guion.colorMarca(), 0, ALTO, guion.colorMarca().darker().darker()));
        g.fillRect(0, 0, ANCHO, ALTO);
        float alfa = (float) Math.min(1.0, t / 0.6);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));

        int tamano = 300;
        int x = (ANCHO - tamano) / 2;
        int y = 300;
        g.setColor(Color.WHITE);
        g.fillOval(x - 12, y - 12, tamano + 24, tamano + 24);
        if (guion.logo() != null) {
            g.setClip(new java.awt.geom.Ellipse2D.Double(x, y, tamano, tamano));
            g.drawImage(guion.logo(), x, y, tamano, tamano, null);
            g.setClip(null);
        } else {
            g.setColor(guion.colorMarca());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 120));
            centrar(g, MarcaDelNegocio.iniciales(guion.nombre()), y + tamano / 2 + 42);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
        int lineaY = y + tamano + 120;
        for (String linea : partir(guion.nombre(), g.getFontMetrics(), ANCHO - 80)) {
            centrar(g, linea, lineaY);
            lineaY += 66;
        }
        if (guion.eslogan() != null && !guion.eslogan().isBlank()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 36));
            for (String linea : partir("“" + guion.eslogan() + "”", g.getFontMetrics(), ANCHO - 100)) {
                centrar(g, linea, lineaY + 10);
                lineaY += 48;
            }
        }
        if (guion.whatsapp() != null) {
            int botonY = ALTO - 260;
            g.setColor(new Color(0x1F, 0x9D, 0x55));
            g.fillRoundRect(90, botonY, ANCHO - 180, 110, 55, 55);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
            centrar(g, "Pide por WhatsApp", botonY + 52);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
            centrar(g, guion.whatsapp(), botonY + 94);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    // El titular del anuncio (imagen, video y afiche): la primera frase del texto que escribió la IA, que ya es un
    // gancho pensado para el público ("Recargue su jornada con estilo"). Antes se usaba lo que escribió el dueño tal
    // cual, y si escribía "Créame un anuncio de…" eso salía en la imagen. Sin texto de la IA, se usa la oferta.
    // HU 67: el titular corto que escribió la IA aparte (3 a 6 palabras); si no hay (anuncios anteriores),
    // la primera frase del texto. Lo usan la imagen, el afiche, el video y Mis anuncios.
    public static String titularDe(String titular, String oferta, String texto) {
        String limpio = titularCorto(titular);
        return limpio != null ? limpio : frasePrincipal(oferta, texto);
    }

    public static String titularDe(com.adbrand.core.contenido.entity.Contenido contenido) {
        return titularDe(contenido.getTitular(), contenido.getOferta(), contenido.getTexto());
    }

    // Deja el titular de la IA en 3 a 6 palabras, sin hashtags, emojis ni comillas. null si no sirve.
    public static String titularCorto(String titular) {
        if (titular == null) {
            return null;
        }
        String limpio = titular
                .replaceAll("#\\S+", " ")
                .replaceAll("[\\p{So}\\p{Cn}\\x{1F000}-\\x{1FFFF}]", " ")
                .replaceAll("[\"“”«»]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[.]+$", "");
        String[] palabras = limpio.isEmpty() ? new String[0] : limpio.split(" ");
        if (palabras.length < 3) {
            return null;
        }
        if (palabras.length > 6) {
            limpio = String.join(" ", java.util.Arrays.copyOf(palabras, 6)).replaceAll("[,;:]$", "");
        }
        return limpio.length() > 80 ? limpio.substring(0, 80).trim() : limpio;
    }

    public static String frasePrincipal(String oferta, String texto) {
        String limpio = texto == null ? "" : texto
                .replaceAll("#\\S+", " ")
                .replaceAll("[\\p{So}\\p{Cn}\\x{1F000}-\\x{1FFFF}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!limpio.isEmpty()) {
            String primera = limpio.split("(?<=[.!?])\\s+")[0].trim().replaceAll("[.]+$", "");
            if (primera.length() > 70) {
                int coma = primera.lastIndexOf(',', 70);
                primera = coma >= 25 ? primera.substring(0, coma) : recortarEnPalabra(primera, 67) + "…";
            }
            if (primera.length() >= 8) {
                return primera;
            }
        }
        String base = oferta == null ? "" : oferta.trim();
        return base.length() > 70 ? recortarEnPalabra(base, 67) + "…" : base;
    }

    private static String recortarEnPalabra(String texto, int maximo) {
        if (texto.length() <= maximo) {
            return texto;
        }
        int espacio = texto.lastIndexOf(' ', maximo);
        return (espacio > 20 ? texto.substring(0, espacio) : texto.substring(0, maximo)).replaceAll("[,;:]$", "").trim();
    }

    private static List<String> partir(String texto, FontMetrics medidas, int anchoMaximo) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : (texto == null ? "" : texto).split("\\s+")) {
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
        return lineas.size() > 4 ? lineas.subList(0, 4) : lineas;
    }

    private static void centrar(Graphics2D g, String texto, int y) {
        g.drawString(texto, (ANCHO - g.getFontMetrics().stringWidth(texto)) / 2, y);
    }
}
