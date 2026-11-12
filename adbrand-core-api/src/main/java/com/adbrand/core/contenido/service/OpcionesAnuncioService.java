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
import com.adbrand.core.ia.service.JsonDeIa;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.ServicioImagenes;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
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

    public OpcionesAnuncioService(PerfilNegocioService perfilService, PlantillaPromptAnuncio plantilla,
                                  ServicioIa ia, ServicioImagenes imagenes, AnuncioService anuncios,
                                  KitMarcaRepository kits, GeneracionAnuncioRepository generaciones,
                                  OpcionAnuncioRepository opciones) {
        this.perfilService = perfilService;
        this.plantilla = plantilla;
        this.ia = ia;
        this.imagenes = imagenes;
        this.anuncios = anuncios;
        this.kits = kits;
        this.generaciones = generaciones;
        this.opciones = opciones;
    }

    @Transactional
    public OpcionesAnuncioResponse generar(Long usuarioId, GenerarAnuncioRequest datos) {
        // HU 10, criterio 1: sin perfil registrado no se puede generar (404 PERFIL_NO_ENCONTRADO).
        PerfilNegocioResponse perfil = perfilService.obtener(usuarioId);
        IdentidadMarca identidad = anuncios.identidadDe(usuarioId);
        List<JsonDeIa.TextoConExtra> textos = pedirTextos(usuarioId, perfil, datos, identidad);
        List<String> colores = coloresDelKit(usuarioId);

        GeneracionAnuncio generacion = new GeneracionAnuncio();
        generacion.setUsuarioId(usuarioId);
        generacion.setOferta(datos.oferta().trim());
        generacion.setRedSocial(datos.redSocial());
        generacion = generaciones.save(generacion);

        List<OpcionAnuncioResponse> respuesta = new ArrayList<>();
        for (int i = 0; i < CANTIDAD; i++) {
            // La escena la escribe la IA de texto en inglés; si no vino, se usa el producto sacado de la oferta.
            String escena = textos.get(i).extra() != null ? textos.get(i).extra() : temaSinCifras(datos.oferta());
            ImagenGenerada imagen = imagenes.borrador(descripcionDeImagen(perfil, escena, ESTILOS[i][1], colores));
            OpcionAnuncio opcion = new OpcionAnuncio();
            opcion.setGeneracion(generacion);
            opcion.setNumero(i + 1);
            opcion.setEstilo(ESTILOS[i][0]);
            opcion.setTexto(textos.get(i).texto());
            opcion.setTextoSimulado(ia.esSimulado());
            opcion.ponerImagen(imagen);
            respuesta.add(OpcionAnuncioResponse.desde(opciones.save(opcion)));
        }
        return new OpcionesAnuncioResponse(generacion.getId(), generacion.getOferta(), generacion.getRedSocial(), respuesta);
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
                generacion.getRedSocial(), opcion.getTexto(), prompt, opcion.isTextoSimulado());

        for (OpcionAnuncio otra : opciones.findByGeneracionIdOrderByNumero(generacion.getId())) {
            otra.setElegida(otra.getId().equals(opcion.getId()));
        }
        opcion.setElegida(true);
        opcion.setContenidoId(anuncio.id());
        return new OpcionElegidaResponse(anuncio, OpcionAnuncioResponse.desde(opciones.save(opcion)));
    }

    // Una sola llamada a la IA de texto pide las 4 versiones en JSON, cada una con su escena para la imagen.
    // Si la respuesta no trae las 4 (o no es JSON, como en el modo simulado), las que faltan se piden de una en una.
    private List<JsonDeIa.TextoConExtra> pedirTextos(Long usuarioId, PerfilNegocioResponse perfil, GenerarAnuncioRequest datos,
                                     IdentidadMarca identidad) {
        PromptAnuncio promptOpciones = plantilla.construirOpciones(perfil, datos.oferta(), datos.redSocial(),
                identidad, CANTIDAD);
        String respuesta = ia.generarTexto(promptOpciones.sistema(), promptOpciones.usuario(), usuarioId,
                OrigenConsumo.ANUNCIO);
        List<JsonDeIa.TextoConExtra> textos = new ArrayList<>(
                JsonDeIa.listaDeObjetos(respuesta, "opciones", "texto", "escena"));

        PromptAnuncio promptUno = plantilla.construir(perfil, datos.oferta(), datos.redSocial(), identidad);
        while (textos.size() < CANTIDAD) {
            String texto = ia.generarTexto(promptUno.sistema(), promptUno.usuario(), usuarioId, OrigenConsumo.ANUNCIO);
            textos.add(new JsonDeIa.TextoConExtra(texto, null));
        }
        return textos.subList(0, CANTIDAD);
    }

    // Los colores del kit de marca más reciente (#RRGGBB), si el negocio tiene kit.
    private List<String> coloresDelKit(Long usuarioId) {
        List<String> colores = new ArrayList<>();
        kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(usuarioId).ifPresent(kit -> {
            Matcher encontrado = COLOR_HEX.matcher(kit.getPaletaColores() == null ? "" : kit.getPaletaColores());
            while (encontrado.find() && colores.size() < 4) {
                colores.add(encontrado.group().toUpperCase());
            }
        });
        return colores;
    }

    // Lo que se le pide a la IA de imágenes. Sin letras: el texto del anuncio va aparte (las IA escriben mal).
    // La oferta va SIN cifras, precios ni descuentos: si la IA ve "2x1" o "S/ 35" intenta escribirlos y salen
    // letras deformes. Solo se le describe el producto y el negocio.
    // "tema" es la escena en inglés que escribió la IA de texto, o el producto sacado de la oferta (temaSinCifras).
    public static String descripcionDeImagen(PerfilNegocioResponse perfil, String tema, String estilo,
                                             List<String> colores) {
        String paleta = colores.isEmpty() ? "" : " Color palette: " + String.join(", ", colores) + ".";
        return ("Appetizing social media picture for a small %s business in Peru (a picture, not a poster or flyer). "
                + "Main subject: %s. Style: %s.%s "
                + "The image must contain absolutely no text: no letters, no numbers, no words, no prices, "
                + "no signs, no labels, no logos and no watermarks.")
                .formatted(perfil.rubro(), tema, estilo, paleta);
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
