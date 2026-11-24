package com.adbrand.core.imagen.service;

import com.adbrand.core.imagen.dto.ImagenGenerada;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Rendimiento: las IA devuelven imágenes pesadas (un PNG de 1024 px puede pasar de 1,5 MB). Antes de guardarlas
// y enviarlas a la web se pasan a JPEG de buena calidad y como máximo 1024 px de lado: pesan varias veces menos,
// la pantalla de anuncios carga más rápido y la base de datos crece menos.
// Si la imagen ya es liviana, es un SVG (la de prueba) o no se puede leer, se devuelve tal cual.
public final class OptimizadorImagen {

    static final int LADO_MAXIMO = 1024;
    static final int PESO_SIN_TOCAR = 150 * 1024;
    private static final float CALIDAD_JPEG = 0.82f;

    private static final Logger log = LoggerFactory.getLogger(OptimizadorImagen.class);

    private OptimizadorImagen() {
    }

    public static ImagenGenerada optimizar(ImagenGenerada imagen) {
        if (imagen == null || imagen.base64() == null || !esMapaDeBits(imagen.tipo())) {
            return imagen;
        }
        try {
            byte[] original = Base64.getDecoder().decode(imagen.base64());
            if (original.length <= PESO_SIN_TOCAR) {
                return imagen;
            }
            BufferedImage leida = ImageIO.read(new ByteArrayInputStream(original));
            if (leida == null) {
                return imagen;
            }
            byte[] comprimida = aJpeg(reducir(leida));
            if (comprimida.length >= original.length) {
                return imagen;
            }
            log.debug("Imagen de {} optimizada: {} KB → {} KB", imagen.proveedor(), original.length / 1024,
                    comprimida.length / 1024);
            return new ImagenGenerada("image/jpeg", Base64.getEncoder().encodeToString(comprimida),
                    imagen.proveedor(), imagen.simulado());
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("No se pudo optimizar la imagen de {}; se usa la original: {}", imagen.proveedor(), ex.getMessage());
            return imagen;
        }
    }

    private static boolean esMapaDeBits(String tipo) {
        return "image/png".equals(tipo) || "image/jpeg".equals(tipo) || "image/jpg".equals(tipo);
    }

    // Lleva la imagen a RGB sobre fondo blanco (JPEG no tiene transparencia) y, si es más grande, a 1024 px de lado.
    private static BufferedImage reducir(BufferedImage leida) {
        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(leida.getWidth(), leida.getHeight()));
        int ancho = Math.max(1, (int) Math.round(leida.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(leida.getHeight() * escala));
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = destino.createGraphics();
        try {
            lienzo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            lienzo.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            lienzo.setColor(Color.WHITE);
            lienzo.fillRect(0, 0, ancho, alto);
            lienzo.drawImage(leida, 0, 0, ancho, alto, null);
        } finally {
            lienzo.dispose();
        }
        return destino;
    }

    private static byte[] aJpeg(BufferedImage imagen) throws IOException {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPEG);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }
}
