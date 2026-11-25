package com.adbrand.core.contenido.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.negocio.repository.PerfilNegocioRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 61: la identidad de la marca que llevan los anuncios (imagen final, afiche, video y lo que se comparte):
// el logo del kit (subido o hecho con IA), sus dos primeros colores y el eslogan elegido.
// Sin kit se usan el nombre del negocio, sus iniciales y los colores de AdBrand.
@Service
public class MarcaDelNegocio {

    static final Color PRINCIPAL_POR_DEFECTO = new Color(0x4F, 0x46, 0xE5);
    static final Color ACENTO_POR_DEFECTO = new Color(0xF9, 0x73, 0x16);
    private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final KitMarcaRepository kits;
    private final PerfilNegocioRepository perfiles;
    private final UsuarioRepository usuarios;

    public MarcaDelNegocio(KitMarcaRepository kits, PerfilNegocioRepository perfiles, UsuarioRepository usuarios) {
        this.kits = kits;
        this.perfiles = perfiles;
        this.usuarios = usuarios;
    }

    // "logo" es la imagen ya leída (para dibujarla); "logoDataUrl" la misma lista para un <img> (null si no hay).
    public record Marca(String nombre, String eslogan, BufferedImage logo, String logoDataUrl, Color principal, Color acento) {

        public boolean tieneEslogan() {
            return eslogan != null && !eslogan.isBlank();
        }

        public String iniciales() {
            return MarcaDelNegocio.iniciales(nombre);
        }
    }

    @Transactional(readOnly = true)
    public Marca de(Long usuarioId) {
        KitMarca kit = kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(usuarioId).orElse(null);
        String nombre = kit != null && noVacio(kit.getNombreNegocio()) ? kit.getNombreNegocio() : nombreDelNegocio(usuarioId);
        List<Color> colores = colores(kit == null ? null : kit.getPaletaColores());
        return new Marca(
                nombre,
                kit == null || !noVacio(kit.getEslogan()) ? null : kit.getEslogan().trim(),
                kit == null ? null : leerBase64(kit.getLogoImagen()),
                kit == null ? null : kit.logoImagenDataUrl(),
                colores.isEmpty() ? PRINCIPAL_POR_DEFECTO : colores.get(0),
                colores.size() < 2 ? ACENTO_POR_DEFECTO : colores.get(1));
    }

    // El nombre comercial del perfil o, si no hay, el de la empresa con que se registró.
    public String nombreDelNegocio(Long usuarioId) {
        var perfil = perfiles.findByUsuarioId(usuarioId).orElse(null);
        if (perfil != null && noVacio(perfil.getNombreComercial())) {
            return perfil.getNombreComercial();
        }
        return usuarios.findById(usuarioId).map(Usuario::getNombreEmpresa).orElse("");
    }

    // Los colores "#RRGGBB" de la paleta del kit, en orden y sin repetir.
    public static List<Color> colores(String paleta) {
        List<Color> colores = new ArrayList<>();
        if (paleta != null) {
            Matcher m = COLOR.matcher(paleta);
            while (m.find()) {
                Color color = Color.decode(m.group());
                if (!colores.contains(color)) {
                    colores.add(color);
                }
            }
        }
        return colores;
    }

    public static String hex(Color color) {
        return "#%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    // Blanco o casi negro, el que se lea mejor sobre ese color (para un kit con colores claros).
    public static Color textoSobre(Color fondo) {
        double luz = (0.299 * fondo.getRed() + 0.587 * fondo.getGreen() + 0.114 * fondo.getBlue()) / 255;
        return luz > 0.62 ? new Color(0x1B, 0x1D, 0x2E) : Color.WHITE;
    }

    public static String iniciales(String nombre) {
        String[] palabras = (nombre == null ? "" : nombre.trim()).split("\\s+");
        if (palabras.length == 0 || palabras[0].isEmpty()) {
            return "AB";
        }
        return palabras.length == 1 ? palabras[0].substring(0, Math.min(2, palabras[0].length())).toUpperCase()
                : ("" + palabras[0].charAt(0) + palabras[1].charAt(0)).toUpperCase();
    }

    public static BufferedImage leerDataUrl(String dataUrl) {
        if (dataUrl == null || !dataUrl.contains(",")) {
            return null;
        }
        return leerBase64(dataUrl.substring(dataUrl.indexOf(',') + 1));
    }

    public static BufferedImage leerBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64.trim())));
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean noVacio(String texto) {
        return texto != null && !texto.isBlank();
    }
}
