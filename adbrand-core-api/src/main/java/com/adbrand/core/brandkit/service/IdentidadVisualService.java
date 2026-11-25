package com.adbrand.core.brandkit.service;

import com.adbrand.core.brandkit.dto.IdentidadVisualDtos.OpcionesLogoResponse;
import com.adbrand.core.brandkit.dto.IdentidadVisualDtos.RetoqueLogoResponse;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.ServicioImagenes;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Identidad de marca (V70): el logo como imagen (subido por la empresa o elegido entre 3 que propone la IA)
// y el eslogan elegido. Los logos de la IA salen de la IA de imágenes gratis (Cloudflare), en estilo plano
// y simple, que es lo que mejor queda en un logo y lo que mejor se imprime.
@Service
public class IdentidadVisualService {

    static final int TAMANO_MAXIMO_LOGO = 2 * 1024 * 1024;
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/(png|jpeg));base64,([A-Za-z0-9+/=\\s]+)$");
    private static final Pattern COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    // Tres estilos distintos para que haya de dónde elegir.
    private static final List<String> ESTILOS_LOGO = List.of(
            "a simple emblem inside a circle",
            "a minimal flat pictogram icon",
            "an abstract geometric brand mark"
    );

    private final KitMarcaRepository kits;
    private final ServicioImagenes imagenes;
    private final ControlGastoImagenes gasto;

    public IdentidadVisualService(KitMarcaRepository kits, ServicioImagenes imagenes, ControlGastoImagenes gasto) {
        this.kits = kits;
        this.imagenes = imagenes;
        this.gasto = gasto;
    }

    // Edita el logo guardado con una instrucción corta, igual que los retoques de anuncios (cuenta como retoque).
    // Devuelve la versión nueva sin guardarla: la empresa decide si la usa.
    @Transactional
    public RetoqueLogoResponse retocarLogo(Long usuarioId, Long kitId, String instruccion) {
        KitMarca kit = buscar(usuarioId, kitId);
        if (kit.getLogoImagen() == null || kit.getLogoImagen().isBlank()) {
            throw new SolicitudInvalidaException("SIN_LOGO", "Primero sube tu logo o elige uno de la IA.");
        }
        if (!imagenes.esSimulado()) {
            gasto.verificarRetoqueDisponible(usuarioId);
        }
        String tipo = kit.getLogoImagenTipo() == null ? "image/png" : kit.getLogoImagenTipo();
        ImagenGenerada actual = new ImagenGenerada(tipo, kit.getLogoImagen(), "kit", false);
        ImagenGenerada nueva = imagenes.finalORetoque(descripcionDeRetoqueLogo(instruccion), actual);
        gasto.registrar(usuarioId, ConsumoImagen.Tipo.RETOQUE, nueva);
        return new RetoqueLogoResponse(nueva.dataUrl(), nueva.simulado(),
                imagenes.esSimulado() ? null : gasto.retoquesRestantes(usuarioId));
    }

    static String descripcionDeRetoqueLogo(String instruccion) {
        return ("Edit this brand logo. Change only this: %s. Keep it a clean, flat, professional logo centered on a "
                + "plain white background, with the same main shape and colors unless the instruction asks otherwise. "
                + "Do not add text, letters or words unless the instruction asks for them.").formatted(instruccion.trim());
    }

    // 3 propuestas de logo hechas por la IA. No se guardan: la empresa elige una y se guarda con guardarLogo.
    @Transactional(readOnly = true)
    public OpcionesLogoResponse opcionesLogo(Long usuarioId, Long kitId) {
        KitMarca kit = buscar(usuarioId, kitId);
        String colores = String.join(", ", coloresDe(kit.getPaletaColores()));
        List<String> opciones = new ArrayList<>();
        for (String estilo : ESTILOS_LOGO) {
            String descripcion = ("Logo icon for \"%s\", a %s business. %s. Design it as %s. Flat vector style, "
                    + "bold simple shapes, 2 or 3 solid colors%s, centered on a plain white background, "
                    + "no text, no letters, no words, no mockup, professional and memorable.").formatted(
                    kit.getNombreNegocio(),
                    kit.getSector(),
                    kit.getLogoConcepto() == null ? "" : "Concept: " + kit.getLogoConcepto(),
                    estilo,
                    colores.isBlank() ? "" : " using " + colores);
            ImagenGenerada imagen = imagenes.borrador(descripcion);
            opciones.add(imagen.dataUrl());
        }
        return new OpcionesLogoResponse(opciones, imagenes.esSimulado());
    }

    // Guarda el logo: el que la empresa ya tenía (SUBIDO) o el que eligió de la IA (IA).
    // Solo imágenes PNG o JPG de verdad (las que se pueden imprimir y poner en el manual PDF) (se revisan los primeros bytes) y de hasta 2 MB.
    @Transactional
    public KitMarcaGeneradoResponse guardarLogo(Long usuarioId, Long kitId, String dataUrl, String origen) {
        KitMarca kit = buscar(usuarioId, kitId);
        Matcher partes = DATA_URL.matcher(dataUrl == null ? "" : dataUrl.trim());
        if (!partes.matches()) {
            throw new SolicitudInvalidaException("LOGO_INVALIDO", "El logo debe ser una imagen PNG o JPG.");
        }
        String tipo = partes.group(1);
        String base64 = partes.group(3).replaceAll("\\s", "");
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("LOGO_INVALIDO", "No pudimos leer la imagen. Prueba con otra.");
        }
        if (bytes.length > TAMANO_MAXIMO_LOGO) {
            throw new SolicitudInvalidaException("LOGO_MUY_GRANDE", "El logo puede pesar hasta 2 MB.");
        }
        if (!esImagen(bytes)) {
            throw new SolicitudInvalidaException("LOGO_INVALIDO", "El archivo no es una imagen PNG o JPG.");
        }
        kit.setLogoImagen(base64);
        kit.setLogoImagenTipo(tipo);
        kit.setLogoOrigen(origen);
        return KitMarcaGeneradoResponse.desde(kits.saveAndFlush(kit), false);
    }

    @Transactional
    public KitMarcaGeneradoResponse elegirEslogan(Long usuarioId, Long kitId, String eslogan) {
        KitMarca kit = buscar(usuarioId, kitId);
        kit.setEslogan(eslogan.trim());
        return KitMarcaGeneradoResponse.desde(kits.saveAndFlush(kit), false);
    }

    @Transactional(readOnly = true)
    public KitMarca buscar(Long usuarioId, Long kitId) {
        return kits.findById(kitId)
                .filter(kit -> kit.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "KIT_MARCA_NO_ENCONTRADO", "Kit de Marca no encontrado: " + kitId));
    }

    static List<String> coloresDe(String paleta) {
        List<String> colores = new ArrayList<>();
        if (paleta == null) {
            return colores;
        }
        Matcher m = COLOR.matcher(paleta);
        while (m.find() && colores.size() < 5) {
            String color = m.group().toUpperCase();
            if (!colores.contains(color)) {
                colores.add(color);
            }
        }
        return colores;
    }

    // Firma de los formatos aceptados: PNG (89 50 4E 47) y JPG (FF D8 FF).
    static boolean esImagen(byte[] b) {
        boolean png = b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean jpg = b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        return png || jpg;
    }
}
