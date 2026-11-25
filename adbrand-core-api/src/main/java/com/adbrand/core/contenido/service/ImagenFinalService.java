package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.contenido.service.MarcaDelNegocio.Marca;
import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 61: la imagen final del anuncio, lista para publicar, con la identidad de la marca: la foto (o un fondo con
// los colores del kit) en el tamaño del formato, la frase de la oferta y abajo una franja del color principal con
// el logo, el nombre y el eslogan, y una línea del color de acento. La usan la vista previa, la descarga, el afiche
// con QR y lo que se comparte desde la web.
@Service
public class ImagenFinalService {

    private final ContenidoRepository contenidos;
    private final PrevisualizacionService previsualizacion;
    private final MarcaDelNegocio marcas;
    private final OpcionAnuncioRepository opciones;
    private final UsuarioRepository usuarios;

    public ImagenFinalService(ContenidoRepository contenidos, PrevisualizacionService previsualizacion,
                              MarcaDelNegocio marcas, OpcionAnuncioRepository opciones, UsuarioRepository usuarios) {
        this.contenidos = contenidos;
        this.previsualizacion = previsualizacion;
        this.marcas = marcas;
        this.opciones = opciones;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public byte[] generar(Long usuarioId, Long contenidoId, FormatoAnuncio formato) {
        return generar(usuarioId, contenidoId, formato, null);
    }

    // "condiciones": la letra chica de INDECOPI que el dueño completó (vigencia y stock). null si no hay.
    @Transactional(readOnly = true)
    public byte[] generar(Long usuarioId, Long contenidoId, FormatoAnuncio formato, String condiciones) {
        Contenido contenido = contenidos.findById(contenidoId)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("ANUNCIO_NO_ENCONTRADO", "Ese anuncio no existe."));
        BufferedImage foto = MarcaDelNegocio.leerDataUrl(previsualizacion.imagen(usuarioId, contenidoId).imagen());
        MarcaDelNegocio.Marca marca = marcas.de(usuarioId);
        // Ajuste 14: si el anuncio salió de las 4 opciones, se dibuja con el mismo diseño de la opción elegida.
        var elegida = opciones.findFirstByContenidoIdAndElegidaTrue(contenidoId);
        if (elegida.isPresent()) {
            boolean conWhatsapp = usuarios.findById(usuarioId)
                    .map(u -> PublicacionService.celularWhatsapp(u.getTelefono()) != null).orElse(false);
            return jpg(DisenoAnuncio.dibujar(foto,
                    DisenoAnuncio.textosDe(contenido.getOferta(), contenido.getTexto(), conWhatsapp).conCondiciones(condiciones), marca,
                    DisenoAnuncio.Plantilla.deOpcion(elegida.get().getNumero()), formato.ancho(), formato.alto()));
        }
        String frase = VideoAnuncioService.frasePrincipal(contenido.getOferta(), contenido.getTexto());
        return jpg(dibujar(foto, frase, marca, formato.ancho(), formato.alto()));
    }

    // La imagen con la marca. Separado para poder probar el diseño sin base de datos.
    public static BufferedImage dibujar(BufferedImage foto, String frase, Marca marca, int ancho, int alto) {
        BufferedImage lienzo = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lienzo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int corto = Math.min(ancho, alto);
        int franja = (int) (corto * 0.17);
        int margen = (int) (ancho * 0.05);
        int arribaFranja = alto - franja;

        // Foto recortada para llenar el formato (sin deformarla) o un degradado con los colores del kit.
        if (foto != null) {
            double escala = Math.max(ancho / (double) foto.getWidth(), arribaFranja / (double) foto.getHeight());
            int w = (int) Math.ceil(foto.getWidth() * escala);
            int h = (int) Math.ceil(foto.getHeight() * escala);
            g.drawImage(foto, (ancho - w) / 2, (arribaFranja - h) / 2, w, h, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, marca.principal(), ancho, arribaFranja, marca.principal().darker()));
            g.fillRect(0, 0, ancho, arribaFranja);
        }

        // La frase de la oferta sobre una sombra para que se lea con cualquier foto.
        if (frase != null && !frase.isBlank()) {
            int inicioSombra = (int) (arribaFranja * 0.5);
            g.setPaint(new GradientPaint(0, inicioSombra, new Color(0, 0, 0, 0), 0, arribaFranja, new Color(0, 0, 0, 190)));
            g.fillRect(0, inicioSombra, ancho, arribaFranja - inicioSombra);
            g.setColor(Color.WHITE);
            int tamano = (int) (corto * (foto == null ? 0.085 : 0.062));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamano));
            List<String> lineas = partir(frase, g.getFontMetrics(), ancho - 2 * margen, 3);
            int alturaLinea = (int) (tamano * 1.2);
            int y = arribaFranja - margen - (lineas.size() - 1) * alturaLinea;
            for (String linea : lineas) {
                g.drawString(linea, margen, y);
                y += alturaLinea;
            }
        }

        // Franja de la marca: color principal, línea de acento arriba, logo en un círculo blanco, nombre y eslogan.
        g.setColor(marca.principal());
        g.fillRect(0, arribaFranja, ancho, franja);
        g.setColor(marca.acento());
        g.fillRect(0, arribaFranja, ancho, Math.max(4, franja / 18));

        int diametro = (int) (franja * 0.68);
        int xLogo = margen;
        int yLogo = arribaFranja + (franja - diametro) / 2 + franja / 36;
        int borde = Math.max(3, diametro / 18);
        g.setColor(Color.WHITE);
        g.fillOval(xLogo - borde, yLogo - borde, diametro + 2 * borde, diametro + 2 * borde);
        if (marca.logo() != null) {
            g.setClip(new Ellipse2D.Double(xLogo, yLogo, diametro, diametro));
            dibujarContenido(g, marca.logo(), xLogo, yLogo, diametro);
            g.setClip(null);
        } else {
            g.setColor(marca.principal());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (diametro * 0.42)));
            FontMetrics m = g.getFontMetrics();
            String iniciales = marca.iniciales();
            g.drawString(iniciales, xLogo + (diametro - m.stringWidth(iniciales)) / 2,
                    yLogo + (diametro - m.getHeight()) / 2 + m.getAscent());
        }

        Color texto = MarcaDelNegocio.textoSobre(marca.principal());
        int xTexto = xLogo + diametro + margen / 2 + borde;
        int anchoTexto = ancho - xTexto - margen;
        Font fuenteNombre = new Font(Font.SANS_SERIF, Font.BOLD, (int) (franja * 0.24));
        Font fuenteEslogan = new Font(Font.SANS_SERIF, Font.ITALIC, (int) (franja * 0.17));
        g.setColor(texto);
        g.setFont(fuenteNombre);
        String nombre = recortar(marca.nombre(), g.getFontMetrics(), anchoTexto);
        int centro = arribaFranja + franja / 2 + franja / 36;
        if (marca.tieneEslogan()) {
            g.drawString(nombre, xTexto, centro - (int) (franja * 0.03));
            g.setFont(fuenteEslogan);
            g.drawString(recortar("“" + marca.eslogan() + "”", g.getFontMetrics(), anchoTexto), xTexto,
                    centro + (int) (franja * 0.2));
        } else {
            g.drawString(nombre, xTexto, centro + g.getFontMetrics().getAscent() / 3);
        }
        g.dispose();
        return lienzo;
    }

    // Dibuja el logo llenando el círculo sin deformarlo (si no es cuadrado, se centra).
    private static void dibujarContenido(Graphics2D g, BufferedImage logo, int x, int y, int lado) {
        double escala = Math.max(lado / (double) logo.getWidth(), lado / (double) logo.getHeight());
        int w = (int) Math.ceil(logo.getWidth() * escala);
        int h = (int) Math.ceil(logo.getHeight() * escala);
        g.setColor(Color.WHITE);
        g.fillRect(x, y, lado, lado);
        g.drawImage(logo, x + (lado - w) / 2, y + (lado - h) / 2, w, h, null);
    }

    static byte[] jpg(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
             ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter escritor = escritores.next();
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(0.9f);
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
            escritor.dispose();
            flujo.flush();
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo crear la imagen del anuncio.", ex);
        }
    }

    private static List<String> partir(String texto, FontMetrics medidas, int anchoMaximo, int maximo) {
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
        if (lineas.size() > maximo) {
            lineas = new ArrayList<>(lineas.subList(0, maximo));
            lineas.set(maximo - 1, recortar(lineas.get(maximo - 1) + "…", medidas, anchoMaximo));
        }
        return lineas;
    }

    // Corta el texto con "…" si no entra en el ancho.
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
}
