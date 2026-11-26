package com.adbrand.core.contenido.service;

import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.dto.IdentidadMarca;
import com.adbrand.core.contenido.dto.OpcionAnuncioResponse;
import com.adbrand.core.contenido.dto.OpcionElegidaResponse;
import com.adbrand.core.contenido.dto.OpcionesAnuncioResponse;
import com.adbrand.core.contenido.dto.PromptAnuncio;
import com.adbrand.core.contenido.entity.GeneracionAnuncio;
import com.adbrand.core.contenido.entity.OpcionAnuncio;
import com.adbrand.core.contenido.repository.GeneracionAnuncioRepository;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.ia.entity.OrigenConsumo;
import com.adbrand.core.ia.service.LimiteConsumoIa;
import com.adbrand.core.ia.service.JsonDeIa;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import com.adbrand.core.imagen.service.ServicioImagenes;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// "Generar → Elegir" (idea del profesor): por cada oferta se generan 4 opciones, cada una con su texto
// (IA de texto, en una sola llamada) y su imagen borrador (IA de imágenes gratis). La PYME elige una
// y recién ahí se guarda como contenido (pasa por el filtro de la HU 13, como cualquier anuncio).
@Service
public class OpcionesAnuncioService {

    public static final int CANTIDAD = 4;

    // Un estilo distinto por opción, para que las 4 imágenes no se parezcan.
    // La descripción va en inglés porque los modelos de imagen la entienden mejor.
    static final String[][] ESTILOS = {
            {"Foto realista", "realistic professional product photography, natural light, shallow depth of field"},
            {"Ilustración", "modern flat vector illustration of the product only, clean shapes, friendly, no typography"},
            {"Minimalista", "minimalist product photo, lots of empty negative space, soft lighting, no typography"},
            {"Colorido", "vibrant colorful product photography, bold saturated colors, dynamic composition, no typography"},
    };

    private static final Pattern COLOR_HEX = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final PerfilNegocioService perfilService;
    private final PlantillaPromptAnuncio plantilla;
    private final ServicioIa ia;
    private final ServicioImagenes imagenes;
    private final AnuncioService anuncios;
    private final KitMarcaRepository kits;
    private final GeneracionAnuncioRepository generaciones;
    private final OpcionAnuncioRepository opciones;
    private final ControlGastoImagenes gasto;

    // HU 67: para saber si la empresa tiene un plan de pago (la escena con su foto usa la IA de pago).
    private final LimiteConsumoIa limites;

    public OpcionesAnuncioService(PerfilNegocioService perfilService, PlantillaPromptAnuncio plantilla,
                                  ServicioIa ia, ServicioImagenes imagenes, AnuncioService anuncios,
                                  KitMarcaRepository kits, GeneracionAnuncioRepository generaciones,
                                  OpcionAnuncioRepository opciones, ControlGastoImagenes gasto) {
        this(perfilService, plantilla, ia, imagenes, anuncios, kits, generaciones, opciones, gasto, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public OpcionesAnuncioService(PerfilNegocioService perfilService, PlantillaPromptAnuncio plantilla,
                                  ServicioIa ia, ServicioImagenes imagenes, AnuncioService anuncios,
                                  KitMarcaRepository kits, GeneracionAnuncioRepository generaciones,
                                  OpcionAnuncioRepository opciones, ControlGastoImagenes gasto,
                                  LimiteConsumoIa limites) {
        this.limites = limites;
        this.perfilService = perfilService;
        this.plantilla = plantilla;
        this.ia = ia;
        this.imagenes = imagenes;
        this.anuncios = anuncios;
        this.kits = kits;
        this.generaciones = generaciones;
        this.opciones = opciones;
        this.gasto = gasto;
    }

    @Transactional
    public OpcionesAnuncioResponse generar(Long usuarioId, GenerarAnuncioRequest datos) {
        // HU 10, criterio 1: sin perfil registrado no se puede generar (404 PERFIL_NO_ENCONTRADO).
        PerfilNegocioResponse perfil = perfilService.obtener(usuarioId);
        IdentidadMarca identidad = anuncios.identidadDe(usuarioId);
        // La foto se revisa antes de pedir los textos, así una foto inválida no gasta tokens.
        ImagenGenerada foto = datos.tieneFoto() ? fotoDelProducto(datos.foto()) : null;
        Textos pedido = pedirTextos(usuarioId, perfil, datos, identidad);
        List<JsonDeIa.TextoConExtra> textos = pedido.textos();
        EstiloMarca marca = estiloDelKit(usuarioId);
        // HU 67, foto del producto: tal cual (FONDO, gratis) o, si el dueño pidió otro ambiente (ESCENA), las
        // opciones 3 y 4 la ponen dentro de una escena nueva con la IA de pago; las 1 y 2 quedan con la foto real.
        boolean quiereEscena = foto != null && "ESCENA".equalsIgnoreCase(pedido.usoFoto());
        String avisoFoto = quiereEscena ? motivoSinEscena(usuarioId) : null;

        GeneracionAnuncio generacion = new GeneracionAnuncio();
        generacion.setUsuarioId(usuarioId);
        generacion.setOferta(datos.oferta().trim());
        generacion.setRedSocial(datos.redSocial());
        generacion = generaciones.save(generacion);

        List<OpcionAnuncioResponse> respuesta = new ArrayList<>();
        for (int i = 0; i < CANTIDAD; i++) {
            // La escena la escribe la IA de texto en inglés; si no vino, se usa el producto sacado de la oferta.
            String escena = textos.get(i).extra() != null ? textos.get(i).extra() : temaSinCifras(datos.oferta());
            ImagenGenerada imagen = foto;
            if (quiereEscena && avisoFoto == null && i >= CANTIDAD - ESCENAS_CON_FOTO) {
                ImagenGenerada escenaIa = escenaConFoto(usuarioId, escena, foto);
                if (escenaIa != null) {
                    imagen = escenaIa;
                } else {
                    avisoFoto = "La IA no pudo armar la escena con tu producto; usamos tu foto tal cual.";
                }
            }
            if (imagen == null) {
                imagen = imagenes.borrador(
                        descripcionDeImagen(perfil, escena, ESTILOS[i][1], marca) + ambienteDeOcasion(datos.ocasion()));
                gasto.registrar(usuarioId, ConsumoImagen.Tipo.BORRADOR, imagen);
            }
            OpcionAnuncio opcion = new OpcionAnuncio();
            opcion.setGeneracion(generacion);
            opcion.setNumero(i + 1);
            opcion.setEstilo(ESTILOS[i][0]);
            opcion.setTexto(textos.get(i).texto());
            opcion.setTitular(com.adbrand.core.contenido.service.VideoAnuncioService.titularCorto(textos.get(i).titular()));
            opcion.setTextoSimulado(ia.esSimulado());
            opcion.ponerImagen(imagen);
            respuesta.add(OpcionAnuncioResponse.desde(opciones.save(opcion)));
        }
        return new OpcionesAnuncioResponse(generacion.getId(), generacion.getOferta(), generacion.getRedSocial(), respuesta,
                avisoFoto);
    }

    // Cuántas de las 4 opciones llevan la escena con la foto (las últimas); el resto, la foto real.
    static final int ESCENAS_CON_FOTO = 2;

    // null si se puede armar la escena; si no, el motivo para avisarle al dueño.
    private String motivoSinEscena(Long usuarioId) {
        if (imagenes.esSimulado()) {
            return null;
        }
        if (limites == null || LimiteConsumoIa.GRATUITO.equals(limites.consumoDelMes(usuarioId).origen())) {
            return "Usamos tu foto tal cual. Poner tu producto en una escena con IA es de los planes de pago.";
        }
        if (gasto.retoquesRestantes(usuarioId) < ESCENAS_CON_FOTO) {
            return "Usamos tu foto tal cual: ya usaste tus retoques con IA de este mes.";
        }
        return null;
    }

    // La escena nueva con el producto de la foto (IA de pago, cuenta como retoque). null si la IA de pago no respondió.
    private ImagenGenerada escenaConFoto(Long usuarioId, String escena, ImagenGenerada foto) {
        String descripcion = "Place the exact product from the reference photo (same shape, colors, packaging and "
                + "details) in this scene: " + escena + ". Professional advertising photo, natural light, the product "
                + "is the clear hero in the center. Absolutely no text, letters, numbers, logos or watermarks.";
        ImagenGenerada resultado = imagenes.finalORetoque(descripcion, foto);
        // Si respondió otra IA (la gratis no usa la foto), no sirve: se usa la foto tal cual.
        if (!imagenes.esSimulado() && !ServicioImagenes.OPENROUTER.equals(resultado.proveedor())) {
            return null;
        }
        gasto.registrar(usuarioId, ConsumoImagen.Tipo.RETOQUE, resultado);
        return resultado;
    }

    private static final java.util.regex.Pattern FOTO =
            java.util.regex.Pattern.compile("^data:(image/(png|jpeg|jpg));base64,([A-Za-z0-9+/=\\s]+)$");

    // La foto que subió el dueño, revisada (que de verdad sea una imagen) y aligerada a 1024 px como las de la IA.
    static ImagenGenerada fotoDelProducto(String dataUrl) {
        java.util.regex.Matcher partes = FOTO.matcher(dataUrl.trim());
        if (!partes.matches()) {
            throw new com.adbrand.core.shared.error.SolicitudInvalidaException("FOTO_INVALIDA",
                    "La foto debe ser JPG o PNG.");
        }
        String base64 = partes.group(3).replaceAll("\\s", "");
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            java.awt.image.BufferedImage leida = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (leida == null) {
                throw new IllegalArgumentException("no es una imagen");
            }
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new com.adbrand.core.shared.error.SolicitudInvalidaException("FOTO_INVALIDA",
                    "No pudimos abrir tu foto. Prueba con otra en JPG o PNG.");
        }
        String tipo = "image/jpg".equals(partes.group(1)) ? "image/jpeg" : partes.group(1);
        return com.adbrand.core.imagen.service.OptimizadorImagen.optimizar(new ImagenGenerada(tipo, base64, "foto", false));
    }

    // Elegir una opción la guarda como contenido (con el filtro de la HU 13). Elegirla otra vez no la duplica.
    @Transactional
    public OpcionElegidaResponse elegir(Long usuarioId, Long opcionId) {
        OpcionAnuncio opcion = opciones.findByIdAndGeneracionUsuarioId(opcionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "OPCION_NO_ENCONTRADA", "La opción de anuncio no existe."));
        GeneracionAnuncio generacion = opcion.getGeneracion();
        PerfilNegocioResponse perfil = perfilService.obtener(usuarioId);
        PromptAnuncio prompt = plantilla.construir(perfil, generacion.getOferta(), generacion.getRedSocial(),
                anuncios.identidadDe(usuarioId));

        AnuncioGeneradoResponse anuncio = anuncios.guardar(usuarioId, perfil, generacion.getOferta(),
                generacion.getRedSocial(), opcion.getTexto(), prompt, opcion.isTextoSimulado(), opcion.getTitular());

        for (OpcionAnuncio otra : opciones.findByGeneracionIdOrderByNumero(generacion.getId())) {
            otra.setElegida(otra.getId().equals(opcion.getId()));
        }
        opcion.setElegida(true);
        opcion.setContenidoId(anuncio.id());
        return new OpcionElegidaResponse(anuncio, OpcionAnuncioResponse.desde(opciones.save(opcion)));
    }

    // HU 55: una nueva versión de la opción elegida se guarda sobre el contenido que
    // ya existía (con su redacción anterior, lista para comparar) y se actualiza la
    // relación opción → anuncio. La imagen elegida no cambia.
    @Transactional
    public OpcionElegidaResponse regenerarTexto(Long usuarioId, Long opcionId) {
        OpcionAnuncio opcion = opciones.findByIdAndGeneracionUsuarioId(opcionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "OPCION_NO_ENCONTRADA", "La opción de anuncio no existe."));
        if (opcion.getContenidoId() == null) {
            throw new SolicitudInvalidaException(
                    "OPCION_NO_ELEGADA", "Primero elige esta opción para pedir una nueva versión.");
        }
        AnuncioGeneradoResponse anuncio = anuncios.regenerar(usuarioId, opcion.getContenidoId());
        opcion.setContenidoId(anuncio.id());
        return new OpcionElegidaResponse(anuncio, OpcionAnuncioResponse.desde(opciones.save(opcion)));
    }

    // Una sola llamada a la IA de texto pide las 4 versiones en JSON, cada una con su escena para la imagen.
    // Si la respuesta no trae las 4 (o no es JSON, como en el modo simulado), las que faltan se piden de una en una.
    // Los textos de las opciones y, si hay foto, cómo la quiere usar el dueño ("FONDO" o "ESCENA").
    private record Textos(List<JsonDeIa.TextoConExtra> textos, String usoFoto) {
    }

    private Textos pedirTextos(Long usuarioId, PerfilNegocioResponse perfil, GenerarAnuncioRequest datos,
                               IdentidadMarca identidad) {
        PromptAnuncio promptOpciones = plantilla.construirOpciones(perfil, datos.oferta(), datos.redSocial(),
                identidad, CANTIDAD, datos.ocasion(), datos.tieneFoto());
        String respuesta = ia.generarTexto(promptOpciones.sistema(), promptOpciones.usuario(), usuarioId,
                OrigenConsumo.ANUNCIO);
        List<JsonDeIa.TextoConExtra> textos = new ArrayList<>(
                JsonDeIa.listaDeObjetos(respuesta, "opciones", "texto", "escena"));

        PromptAnuncio promptUno = plantilla.construir(perfil, datos.oferta(), datos.redSocial(), identidad, datos.ocasion());
        while (textos.size() < CANTIDAD) {
            String texto = ia.generarTexto(promptUno.sistema(), promptUno.usuario(), usuarioId, OrigenConsumo.ANUNCIO);
            textos.add(new JsonDeIa.TextoConExtra(texto, null));
        }
        return new Textos(textos.subList(0, CANTIDAD), JsonDeIa.campo(respuesta, "usoFoto"));
    }

    // Lo que el kit de marca más reciente aporta a la imagen: hasta 4 colores (#RRGGBB) y el estilo visual.
    // Así las 4 opciones se ven de la misma marca. Sin kit, la imagen se genera solo con el rubro.
    public record EstiloMarca(List<String> colores, String estiloVisual) {
        public static final EstiloMarca SIN_KIT = new EstiloMarca(List.of(), null);
    }

    private EstiloMarca estiloDelKit(Long usuarioId) {
        return kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(usuarioId)
                .map(kit -> {
                    List<String> colores = new ArrayList<>();
                    Matcher encontrado = COLOR_HEX.matcher(kit.getPaletaColores() == null ? "" : kit.getPaletaColores());
                    while (encontrado.find() && colores.size() < 4) {
                        colores.add(encontrado.group().toUpperCase());
                    }
                    return new EstiloMarca(colores, kit.getEstiloVisual());
                })
                .orElse(EstiloMarca.SIN_KIT);
    }

    // El estilo visual del kit (lo elige el dueño al pedirlo) traducido al "ánimo" que entiende la IA de imágenes.
    static final java.util.Map<String, String> ANIMO_POR_ESTILO = java.util.Map.of(
            "MINIMALISTA", "clean, calm and minimal",
            "MODERNO", "modern, fresh and bright",
            "CLASICO", "classic, warm and timeless",
            "DIVERTIDO", "playful, cheerful and energetic",
            "ELEGANTE", "elegant, refined and premium",
            "TECNOLÓGICO", "sleek, futuristic and high-tech",
            "ARTESANAL", "handcrafted, warm and rustic",
            "CORPORATIVO", "professional, trustworthy and neat");

    // Rubros de comida: ahí sí se pide que la imagen "dé hambre". Para ropa, tecnología o servicios, no.
    static boolean esDeComida(String rubro) {
        String texto = rubro == null ? "" : rubro.toLowerCase(java.util.Locale.ROOT);
        return texto.matches(".*(panader|pasteler|restaurant|comida|cafeter|café|cafe|bodega|minimarket|dulce|postre).*");
    }

    // Lo que se le pide a la IA de imágenes. Sin letras: el texto del anuncio va aparte (las IA escriben mal).
    // La oferta va SIN cifras, precios ni descuentos: si la IA ve "2x1" o "S/ 35" intenta escribirlos y salen
    // letras deformes. Solo se le describe el producto y el negocio.
    // "tema" es la escena en inglés que escribió la IA de texto, o el producto sacado de la oferta (temaSinCifras).
    // Ajuste de la revisión: si el anuncio es para una fecha especial (se creó desde el calendario), la imagen se
    // ambienta en ella con sus decoraciones típicas. Así un anuncio de Navidad no sale con hojas de otoño.
    private static final java.util.Map<String, String> AMBIENTE_POR_OCASION = java.util.Map.ofEntries(
            java.util.Map.entry("navidad", "Christmas in Peru: warm string lights, pine branches, red and gold ornaments, panetón on the table"),
            java.util.Map.entry("halloween", "Halloween: carved pumpkins, candles, orange and purple tones, playful and friendly (not scary)"),
            java.util.Map.entry("black friday", "Black Friday shopping mood: black and gold accents, gift boxes and shopping bags"),
            java.util.Map.entry("madre", "Mother's Day: fresh flowers, soft pink tones, a wrapped gift"),
            java.util.Map.entry("padre", "Father's Day: warm tones, a wrapped gift, cozy family setting"),
            java.util.Map.entry("fiestas patrias", "Peruvian Independence Day: red and white Peruvian flag colors and rosettes (escarapelas)"),
            java.util.Map.entry("valentin", "Valentine's Day: hearts, red roses, warm romantic light"),
            java.util.Map.entry("cancion criolla", "Peruvian creole music night: cajon drum, guitar, festive warm lights"),
            java.util.Map.entry("muertos", "Peruvian Day of the Dead: flowers, candles and traditional tantawawa bread"),
            java.util.Map.entry("ano nuevo", "New Year's Eve: golden lights, confetti, yellow accents"),
            java.util.Map.entry("fin de ano", "New Year's Eve: golden lights, confetti, yellow accents"),
            java.util.Map.entry("del pan", "World Bread Day: freshly baked bread, rustic wooden table, flour"),
            java.util.Map.entry("del cafe", "Peruvian Coffee Day: coffee beans, steaming cups, morning light"));

    public static String ambienteDeOcasion(String ocasion) {
        if (ocasion == null || ocasion.isBlank()) {
            return "";
        }
        String clave = java.text.Normalizer.normalize(ocasion, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        String ambiente = AMBIENTE_POR_OCASION.entrySet().stream()
                .filter(e -> clave.contains(e.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(ocasion.trim() + " in Peru, with its typical decorations, colors and symbols");
        return " Seasonal theme (make it clearly visible): " + ambiente + ". Still no text of any kind.";
    }

    public static String descripcionDeImagen(PerfilNegocioResponse perfil, String tema, String estilo,
                                             List<String> colores) {
        return descripcionDeImagen(perfil, tema, estilo, new EstiloMarca(colores, null));
    }

    public static String descripcionDeImagen(PerfilNegocioResponse perfil, String tema, String estilo,
                                             EstiloMarca marca) {
        String tipo = esDeComida(perfil.rubro()) ? "Appetizing" : "Eye-catching";
        String paleta = marca.colores().isEmpty() ? ""
                : " Brand color palette: " + String.join(", ", marca.colores())
                + ". Use these colors in the background, props and lighting so the picture feels on-brand.";
        String animo = marca.estiloVisual() == null ? null : ANIMO_POR_ESTILO.get(marca.estiloVisual().toUpperCase());
        String ambiente = animo == null ? "" : " Brand mood: " + animo + ".";
        return ("%s social media picture for a small %s business in Peru (a picture, not a poster or flyer). "
                + "Main subject: %s. Style: %s.%s%s "
                + "The image must contain absolutely no text: no letters, no numbers, no words, no prices, "
                + "no signs, no labels, no logos and no watermarks.")
                .formatted(tipo, perfil.rubro(), tema, estilo, paleta, ambiente);
    }

    // Solo el producto: "2x1 en pan de yema este sábado" → "pan de yema";
    // "2 panetones por S/ 35 hasta el 24 de diciembre" → "panetones".
    // Se quitan cifras, precios, palabras de oferta, la preposición del inicio y todo lo que viene desde
    // un conector o una fecha ("este sábado", "por", "hasta…"), porque la IA tiende a escribir esa frase.
    public static String temaSinCifras(String oferta) {
        String tema = oferta == null ? "" : oferta
                .replaceAll("(?i)s/\\s*\\d+([.,]\\d+)?", " ")
                .replaceAll("(?i)\\b\\d+\\s*[x×]\\s*\\d+\\b", " ")
                .replaceAll("\\d+([.,]\\d+)?\\s*%?", " ")
                .replaceAll("(?iu)(?<!\\p{L})(dscto|descuentos?|ofertas?|promo|promoción|precios?|soles|gratis)(?!\\p{L})", " ")
                .replaceAll("[%$€!¡.,:;]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        tema = tema.replaceFirst("(?iu)^((en|de|del|por|para|con|a|al|y|la|el|los|las)\\s+)+", "");
        tema = tema.replaceFirst("(?iu)\\s+(este|esta|estos|estas|hasta|desde|solo|sólo|todos|todas|cada|hoy|mañana"
                + "|por|con|para|del|al|y|a|en|lunes|martes|miércoles|jueves|viernes|sábado|domingo)(\\s.*)?$", "");
        tema = tema.trim();
        return tema.isBlank() ? "the products of the business" : tema;
    }
}
