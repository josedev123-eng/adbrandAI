package com.adbrand.core.contenido.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.dto.MarcaPublicacion;
import com.adbrand.core.contenido.dto.PublicacionResponse;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.ia.service.LimiteConsumoIa;
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
    private final LimiteConsumoIa limites;

    public PublicacionService(ContenidoRepository contenidos, PerfilNegocioRepository perfiles, UsuarioRepository usuarios,
                              MarcaDelNegocio marcas, LimiteConsumoIa limites) {
        this.contenidos = contenidos;
        this.perfiles = perfiles;
        this.usuarios = usuarios;
        this.marcas = marcas;
        this.limites = limites;
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

        List<String> hashtags = hashtags(contenido.getTexto(), nombre, rubro, contenido.getOferta());
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
                RevisionLegalOferta.revisar(contenido.getOferta(), contenido.getTexto()), MarcaPublicacion.desde(marca),
                !LimiteConsumoIa.GRATUITO.equals(limites.consumoDelMes(usuarioId).origen()));
    }

    // HU 61: "— Panadería Rosa: “El sabor de tu barrio”". Sin eslogan no se agrega nada (el nombre ya va en los hashtags).
    public static String firma(String nombre, String eslogan) {
        if (eslogan == null || eslogan.isBlank()) {
            return null;
        }
        String limpio = eslogan.trim().replaceAll("^[\"“”]+|[\"“”]+$", "");
        return nombre == null || nombre.isBlank() ? "— “" + limpio + "”" : "— " + nombre.trim() + ": “" + limpio + "”";
    }

    // Hashtags de festividades: si el anuncio habla de una fecha (o se creó desde el calendario con esa ocasión),
    // van los que la gente busca esos días. Las palabras van sin tildes y en minúsculas.
    private static final Map<Pattern, List<String>> POR_FESTIVIDAD = festividades();

    private static Map<Pattern, List<String>> festividades() {
        Map<Pattern, List<String>> mapa = new LinkedHashMap<>();
        mapa.put(Pattern.compile("navidad|navideñ|naviden|nochebuena|panet[oó]n|panetones"), List.of("#Navidad", "#NavidadPeruana"));
        mapa.put(Pattern.compile("halloween|dulce o truco"), List.of("#Halloween", "#HalloweenPeru"));
        mapa.put(Pattern.compile("cancion criolla|criollo|criolla"), List.of("#DiaDeLaCancionCriolla", "#MusicaCriolla"));
        mapa.put(Pattern.compile("dia de los muertos|todos los santos|difuntos|tantawawa"), List.of("#DiaDeLosMuertos", "#TodosLosSantos"));
        mapa.put(Pattern.compile("dia de la madre|para mama|las mamas"), List.of("#DiaDeLaMadre", "#FelizDiaMama"));
        mapa.put(Pattern.compile("dia del padre|para papa|los papas"), List.of("#DiaDelPadre", "#FelizDiaPapa"));
        mapa.put(Pattern.compile("fiestas patrias|28 de julio|independencia"), List.of("#FiestasPatrias", "#OrgulloPeruano"));
        mapa.put(Pattern.compile("san valentin|amor y la amistad|14 de febrero"), List.of("#SanValentin", "#AmorYAmistad"));
        mapa.put(Pattern.compile("ano nuevo|fin de ano"), List.of("#AnoNuevo", "#FinDeAno"));
        mapa.put(Pattern.compile("black friday"), List.of("#BlackFriday", "#BlackFridayPeru"));
        mapa.put(Pattern.compile("cyber ?wow|cyber days"), List.of("#CyberWow"));
        mapa.put(Pattern.compile("dia (mundial )?del pan"), List.of("#DiaDelPan"));
        mapa.put(Pattern.compile("gastronomia peruana|dia de la gastronomia"), List.of("#GastronomiaPeruana"));
        mapa.put(Pattern.compile("dia del cafe"), List.of("#DiaDelCafePeruano"));
        mapa.put(Pattern.compile("santa rosa"), List.of("#SantaRosaDeLima"));
        mapa.put(Pattern.compile("primavera"), List.of("#Primavera"));
        mapa.put(Pattern.compile("regreso a clases|utiles escolares|vuelta al cole"), List.of("#RegresoAClases"));
        mapa.put(Pattern.compile("carnaval"), List.of("#Carnavales"));
        return mapa;
    }

    // Compatibilidad: sin la oferta (las pruebas y lugares que solo tienen el texto).
    public static List<String> hashtags(String texto, String nombre, String rubro) {
        return hashtags(texto, nombre, rubro, null);
    }

    // Hasta 8, sin repetir y en este orden: la marca, la festividad (si la hay), el producto de la oferta,
    // los que puso la IA (máximo 2), los del rubro y #ComproLocal / #HechoEnPeru al final.
    public static List<String> hashtags(String texto, String nombre, String rubro, String oferta) {
        Map<String, String> unicos = new LinkedHashMap<>();
        agregar(unicos, hashtagDe(nombre));

        String buscar = sinTildes(((oferta == null ? "" : oferta) + " " + (texto == null ? "" : texto))).toLowerCase(Locale.ROOT);
        POR_FESTIVIDAD.forEach((patron, etiquetas) -> {
            if (patron.matcher(buscar).find()) {
                etiquetas.forEach(e -> agregar(unicos, e));
            }
        });

        if (oferta != null) {
            String producto = OpcionesAnuncioService.temaSinCifras(oferta);
            // Solo si es corto ("panetones", "cafe americano"); una frase larga no sirve como hashtag.
            if (!producto.isBlank() && producto.split("\\s+").length <= 2) {
                agregar(unicos, hashtagDe(producto));
            }
        }

        int deLaIa = 0;
        Matcher m = HASHTAG.matcher(texto == null ? "" : texto);
        while (m.find() && deLaIa < 2) {
            String limpio = "#" + sinTildes(m.group().substring(1));
            if (!unicos.containsKey(limpio.toLowerCase(Locale.ROOT))) {
                agregar(unicos, limpio);
                deLaIa++;
            }
        }
        if (rubro != null) {
            String clave = sinTildes(rubro).toLowerCase(Locale.ROOT);
            POR_RUBRO.forEach((parte, etiquetas) -> {
                if (clave.contains(parte)) {
                    etiquetas.forEach(e -> agregar(unicos, e));
                }
            });
        }
        // Los de comprar local se reservan un lugar aunque ya haya 8.
        List<String> lista = new ArrayList<>(unicos.values());
        List<String> locales = SIEMPRE.stream().filter(e -> !unicos.containsKey(e.toLowerCase(Locale.ROOT))).toList();
        int espacio = Math.max(0, MAXIMO_HASHTAGS - Math.min(locales.size(), 2));
        List<String> salida = new ArrayList<>(lista.subList(0, Math.min(espacio, lista.size())));
        for (String local : locales) {
            if (salida.size() < MAXIMO_HASHTAGS) {
                salida.add(local);
            }
        }
        return salida;
    }

    private static void agregar(Map<String, String> unicos, String etiqueta) {
        if (etiqueta != null && etiqueta.length() > 1) {
            unicos.putIfAbsent(etiqueta.toLowerCase(Locale.ROOT), etiqueta);
        }
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
