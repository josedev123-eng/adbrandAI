package com.adbrand.core.contenido.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.dto.MarcaPublicacion;
import com.adbrand.core.contenido.dto.PublicacionResponse;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.repository.PerfilNegocioRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Listo para publicar: el texto del anuncio con sus hashtags (marca + rubro + Perú) y, si la empresa tiene celular,
// el enlace "Pedir por WhatsApp" (wa.me) con el mensaje ya escrito. WhatsApp es la red n.º 1 de las mipymes (43 %,
// PRODUCE 2024), así que el cliente pide con un toque. No usa IA: no gasta tokens.
@Service
public class PublicacionService {

    static final int MAXIMO_HASHTAGS = 8;
    private static final Pattern HASHTAG = Pattern.compile("#[\\p{L}\\p{N}_]+");
    private static final Pattern CELULAR_PERU = Pattern.compile("^9\\d{8}$");

    // Hashtags que más se usan por rubro (la misma lista de rubros que la web y la app).
    private static final Map<String, List<String>> POR_RUBRO = Map.ofEntries(
            Map.entry("panader", List.of("#Panaderia", "#PanCaliente")),
            Map.entry("restaurante", List.of("#ComidaPeruana", "#Restaurante")),
            Map.entry("bodega", List.of("#Bodega", "#Minimarket")),
            Map.entry("ropa", List.of("#Moda", "#RopaPeru")),
            Map.entry("belleza", List.of("#Belleza", "#CuidadoPersonal")),
            Map.entry("salud", List.of("#Salud", "#Bienestar")),
            Map.entry("tecnolog", List.of("#Tecnologia", "#Tech")),
            Map.entry("educaci", List.of("#Educacion", "#Clases")),
            Map.entry("servicio", List.of("#Servicios", "#Profesionales"))
    );
    private static final List<String> SIEMPRE = List.of("#ComproLocal", "#HechoEnPeru");

    private final ContenidoRepository contenidos;
    private final PerfilNegocioRepository perfiles;
    private final UsuarioRepository usuarios;
    private final MarcaDelNegocio marcas;

    public PublicacionService(ContenidoRepository contenidos, PerfilNegocioRepository perfiles, UsuarioRepository usuarios,
                              MarcaDelNegocio marcas) {
        this.contenidos = contenidos;
        this.perfiles = perfiles;
        this.usuarios = usuarios;
        this.marcas = marcas;
    }

    @Transactional(readOnly = true)
    public PublicacionResponse preparar(Long usuarioId, Long contenidoId) {
        Contenido contenido = contenidos.findById(contenidoId)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("ANUNCIO_NO_ENCONTRADO", "Ese anuncio no existe."));
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        var perfil = perfiles.findByUsuarioId(usuarioId).orElse(null);

        String nombre = perfil != null && perfil.getNombreComercial() != null ? perfil.getNombreComercial()
                : usuario != null ? usuario.getNombreEmpresa() : "";
        String rubro = perfil != null ? perfil.getRubro() : null;

        List<String> hashtags = hashtags(contenido.getTexto(), nombre, rubro);
        String cuerpo = sinHashtags(contenido.getTexto());

        String telefono = usuario == null ? null : celularWhatsapp(usuario.getTelefono());
        String mensajePedido = "Hola %s, vi su anuncio y quiero: %s".formatted(nombre, contenido.getOferta()).trim();
        String enlace = telefono == null ? null
                : "https://wa.me/" + telefono + "?text=" + URLEncoder.encode(mensajePedido, StandardCharsets.UTF_8).replace("+", "%20");

        // HU 61: la firma de la marca con su eslogan va después del texto (antes del WhatsApp y los hashtags).
        MarcaDelNegocio.Marca marca = marcas.de(usuarioId);
        StringBuilder texto = new StringBuilder(cuerpo);
        String firma = firma(marca.nombre(), marca.eslogan());
        if (firma != null) {
            texto.append("\n\n").append(firma);
        }
        if (telefono != null) {
            // En el texto va el enlace corto (se ve limpio en el post); el QR y el botón usan el que trae el mensaje.
            texto.append("\n\n📲 Pide por WhatsApp: wa.me/").append(telefono);
        }
        if (!hashtags.isEmpty()) {
            texto.append("\n\n").append(String.join(" ", hashtags));
        }
        return new PublicacionResponse(contenido.getId(), texto.toString(), hashtags, enlace, mensajePedido, telefono != null,
                RevisionLegalOferta.revisar(contenido.getOferta(), contenido.getTexto()), MarcaPublicacion.desde(marca));
    }

    // HU 61: "— Panadería Rosa: “El sabor de tu barrio”". Sin eslogan no se agrega nada (el nombre ya va en los hashtags).
    public static String firma(String nombre, String eslogan) {
        if (eslogan == null || eslogan.isBlank()) {
            return null;
        }
        String limpio = eslogan.trim().replaceAll("^[\"“”]+|[\"“”]+$", "");
        return nombre == null || nombre.isBlank() ? "— “" + limpio + "”" : "— " + nombre.trim() + ": “" + limpio + "”";
    }

    // Los del texto de la IA + el de la marca + los del rubro + los de Perú, sin repetir, hasta 8.
    public static List<String> hashtags(String texto, String nombre, String rubro) {
        Map<String, String> unicos = new LinkedHashMap<>();
        String marca = hashtagDe(nombre);
        if (marca != null) {
            unicos.put(marca.toLowerCase(Locale.ROOT), marca);
        }
        Matcher m = HASHTAG.matcher(texto == null ? "" : texto);
        while (m.find()) {
            String limpio = "#" + sinTildes(m.group().substring(1));
            unicos.putIfAbsent(limpio.toLowerCase(Locale.ROOT), limpio);
        }
        if (rubro != null) {
            String clave = sinTildes(rubro).toLowerCase(Locale.ROOT);
            POR_RUBRO.forEach((parte, etiquetas) -> {
                if (clave.contains(parte)) {
                    etiquetas.forEach(e -> unicos.putIfAbsent(e.toLowerCase(Locale.ROOT), e));
                }
            });
        }
        SIEMPRE.forEach(e -> unicos.putIfAbsent(e.toLowerCase(Locale.ROOT), e));
        return new ArrayList<>(unicos.values()).subList(0, Math.min(MAXIMO_HASHTAGS, unicos.size()));
    }

    // "Panadería Doña Rosa" -> "#PanaderiaDonaRosa".
    public static String hashtagDe(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        StringBuilder etiqueta = new StringBuilder("#");
        for (String palabra : sinTildes(nombre).split("[^A-Za-z0-9]+")) {
            if (!palabra.isEmpty()) {
                etiqueta.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1));
            }
        }
        return etiqueta.length() > 1 ? etiqueta.toString() : null;
    }

    // El texto sin los hashtags (se agregan ordenados al final), sin espacios ni líneas de más.
    public static String sinHashtags(String texto) {
        if (texto == null) {
            return "";
        }
        return HASHTAG.matcher(texto).replaceAll("")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    // Solo celulares peruanos (9 dígitos que empiezan en 9): un teléfono fijo no tiene WhatsApp.
    public static String celularWhatsapp(String telefono) {
        if (telefono == null) {
            return null;
        }
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.startsWith("51") && digitos.length() == 11) {
            digitos = digitos.substring(2);
        }
        return CELULAR_PERU.matcher(digitos).matches() ? "51" + digitos : null;
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace("ñ", "n").replace("Ñ", "N");
    }
}
