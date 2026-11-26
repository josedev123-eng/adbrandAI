package com.adbrand.core.video.service;

import com.adbrand.core.contenido.service.VideoAnuncioService;
import com.adbrand.core.contenido.service.VideoAnuncioService.Guion;
import java.awt.AlphaComposite;
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
import org.jcodec.api.FrameGrab;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

// HU 66: al video que hizo la IA (la foto del anuncio en movimiento) se le pone encima el titular del anuncio
// (la IA nunca escribe el texto, así no sale mal escrito) y al final 3 segundos con la marca: logo, nombre, eslogan y
// "Pide por WhatsApp", igual que el video de plantilla. Queda vertical de 720 x 1280.
public final class ComposicionVideoIa {

    static final int ANCHO = 720;
    static final int ALTO = 1280;
    static final double SEGUNDOS_MARCA = 3.0;

    private ComposicionVideoIa() {
    }

    public static byte[] componer(byte[] videoIa, Guion guion) {
        File entrada = null;
        File salida = null;
        try {
            entrada = File.createTempFile("adbrand-ia-", ".mp4");
            Files.write(entrada.toPath(), videoIa);
            salida = File.createTempFile("adbrand-ia-final-", ".mp4");

            FrameGrab lector = FrameGrab.createFrameGrab(NIOUtils.readableChannel(entrada));
            var meta = lector.getVideoTrack().getMeta();
            int fps = 24;
            if (meta != null && meta.getTotalFrames() > 0 && meta.getTotalDuration() > 0) {
                fps = (int) Math.max(12, Math.min(60, Math.round(meta.getTotalFrames() / meta.getTotalDuration())));
            }

            AWTSequenceEncoder codificador = AWTSequenceEncoder.createSequenceEncoder(salida, fps);
            Picture cuadro;
            int numero = 0;
            while ((cuadro = lector.getNativeFrame()) != null) {
                BufferedImage original = AWTUtil.toBufferedImage(cuadro);
                codificador.encodeImage(conTitular(original, guion, numero / (double) fps));
                numero++;
            }
            // Cierre de marca.
            int cuadrosMarca = (int) (SEGUNDOS_MARCA * fps);
            for (int i = 0; i < cuadrosMarca; i++) {
                codificador.encodeImage(VideoAnuncioService.cuadroMarca(guion, i / (double) fps));
            }
            codificador.finish();
            return Files.readAllBytes(salida.toPath());
        } catch (IOException | RuntimeException | org.jcodec.api.JCodecException ex) {
            // Si no se puede abrir el video de la IA (un formato que JCodec no lee), se entrega tal cual.
            return videoIa;
        } finally {
            if (entrada != null) {
                entrada.delete();
            }
            if (salida != null) {
                salida.delete();
            }
        }
    }

    // Un cuadro de la IA, llevado a 720 x 1280 (recortado al centro si no es 9:16), con el titular que aparece suave.
    static BufferedImage conTitular(BufferedImage original, Guion guion, double t) {
        BufferedImage lienzo = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = lienzo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double escala = Math.max(ANCHO / (double) original.getWidth(), ALTO / (double) original.getHeight());
        int w = (int) Math.ceil(original.getWidth() * escala);
        int h = (int) Math.ceil(original.getHeight() * escala);
        g.drawImage(original, (ANCHO - w) / 2, (ALTO - h) / 2, w, h, null);

        if (guion.frase() != null && !guion.frase().isBlank()) {
            g.setPaint(new GradientPaint(0, ALTO * 0.55f, new Color(0, 0, 0, 0), 0, ALTO, new Color(0, 0, 0, 190)));
            g.fillRect(0, (int) (ALTO * 0.55), ANCHO, (int) (ALTO * 0.45));
            double aparicion = Math.min(1.0, Math.max(0.0, (t - 0.6) / 0.8));
            if (aparicion > 0) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) aparicion));
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 50));
                List<String> lineas = partir(guion.frase(), g.getFontMetrics(), ANCHO - 100);
                int y = ALTO - 170 - (lineas.size() - 1) * 62 + (int) ((1 - aparicion) * 50);
                for (String linea : lineas) {
                    g.drawString(linea, 50, y);
                    y += 62;
                }
                g.setColor(guion.colorMarca());
                g.fillRoundRect(50, y - 28, 120, 10, 10, 10);
                g.setComposite(AlphaComposite.SrcOver);
            }
        }
        g.dispose();
        return lienzo;
    }

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
        return lineas.size() > 3 ? lineas.subList(0, 3) : lineas;
    }
}
