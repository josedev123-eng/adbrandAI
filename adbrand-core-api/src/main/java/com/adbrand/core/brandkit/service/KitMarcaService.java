package com.adbrand.core.brandkit.service;

import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.dto.KitMarcaPrompt;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.ia.entity.OrigenConsumo;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Service: orquesta perfil + plantilla + IA + guardado (HU 11).
@Service
public class KitMarcaService {

    private final PerfilNegocioService perfilService;
    private final PlantillaPromptKitMarca plantilla;
    private final ServicioIa ia;
    private final KitMarcaRepository kits;

    public KitMarcaService(PerfilNegocioService perfilService, PlantillaPromptKitMarca plantilla,
                           ServicioIa ia, KitMarcaRepository kits) {
        this.perfilService = perfilService;
        this.plantilla = plantilla;
        this.ia = ia;
        this.kits = kits;
    }

    @Transactional
    public KitMarcaGeneradoResponse generar(Long usuarioId, GenerarKitMarcaRequest datos) {
        // HU 11: sin perfil registrado no se puede generar (404 PERFIL_NO_ENCONTRADO).
        PerfilNegocioResponse perfil = perfilService.obtener(usuarioId);

        KitMarcaPrompt prompt = plantilla.construir(perfil, datos);
        // HU 17: los tokens usados se suman a la empresa que pidió el kit.
        String respuestaJson = ia.generarTexto(prompt.sistema(), prompt.usuario(), usuarioId, OrigenConsumo.KIT_MARCA);

        // Parsear JSON de la IA
        KitMarca kit = parsearYGuardar(usuarioId, datos, respuestaJson);

        return KitMarcaGeneradoResponse.desde(kit, ia.esSimulado());
    }

    private KitMarca parsearYGuardar(Long usuarioId, GenerarKitMarcaRequest datos, String json) {
        // La IA debería devolver JSON puro. En modo simulado, el JSON ya viene bien formado.
        // Usamos Jackson manualmente para evitar dependencia extra en el DTO.
        try {
            tools.jackson.databind.JsonNode node =
                    new tools.jackson.databind.ObjectMapper().readTree(sinBloqueMarkdown(json));

            KitMarca kit = new KitMarca();
            kit.setUsuarioId(usuarioId);
            kit.setNombreNegocio(datos.nombreNegocio());
            kit.setSector(datos.sector());
            kit.setPaletaSugerida(datos.paletaSugerida());
            kit.setValoresEslogan(datos.valoresEslogan());
            kit.setEstiloVisual(datos.estiloVisual());
            kit.setLogoConcepto(comoTexto(node.get("logo_concepto")));
            kit.setTipografiaPrimaria(comoTexto(node.get("tipografia_primaria")));
            kit.setTipografiaSecundaria(comoTexto(node.get("tipografia_secundaria")));
            kit.setPaletaColores(comoTexto(node.get("paleta_colores")));
            kit.setVozMarca(comoTexto(node.get("voz_marca")));
            guardarIdentidad(kit, datos, node.get("eslogan_opciones"));

            return kits.save(kit);
        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta de IA para Kit de Marca: " + e.getMessage(), e);
        }
    }

    // Identidad de marca (V70): si la empresa ya tenía eslogan se respeta tal cual; si no, se guardan las
    // propuestas de la IA y la primera queda elegida hasta que la empresa escoja otra.
    public static void guardarIdentidad(KitMarca kit, GenerarKitMarcaRequest datos, tools.jackson.databind.JsonNode opciones) {
        kit.setSentimiento(recortar(datos.sentimiento(), 100));
        kit.setDiferencial(recortar(datos.diferencial(), 300));
        if (datos.tieneEslogan()) {
            String propio = recortar(datos.eslogan(), 150);
            kit.setEslogan(propio);
            kit.setEsloganOpciones(propio);
            return;
        }
        java.util.List<String> propuestas = new java.util.ArrayList<>();
        if (opciones != null && opciones.isArray()) {
            for (tools.jackson.databind.JsonNode opcion : opciones) {
                String texto = recortar(opcion.asText().replace("\"", "").replace("\n", " "), 150);
                if (texto != null && !propuestas.contains(texto)) {
                    propuestas.add(texto);
                }
            }
        } else if (opciones != null && !opciones.isNull()) {
            opciones.asText().lines().map(linea -> recortar(linea, 150)).filter(java.util.Objects::nonNull)
                    .limit(3).forEach(propuestas::add);
        }
        java.util.List<String> tres = propuestas.stream().limit(3).toList();
        kit.setEsloganOpciones(tres.isEmpty() ? null : String.join("\n", tres));
        kit.setEslogan(tres.isEmpty() ? null : tres.get(0));
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo).trim() : limpio;
    }

    // Algunas IA (por ejemplo Gemini) envuelven el JSON en un bloque ```json ... ```; se quita antes de leerlo.
    static String sinBloqueMarkdown(String respuesta) {
        return com.adbrand.core.ia.service.JsonDeIa.sinBloqueMarkdown(respuesta);
    }

    // Cada campo se guarda como texto. Si la IA manda una lista (por ejemplo, los colores de la paleta),
    // se guarda un elemento por línea, que es el formato que leen la web y la edición del kit.
    // Si un elemento es un objeto ({"hex": ..., "nombre": ...}), sus valores se unen con " - ".
    static String comoTexto(tools.jackson.databind.JsonNode valor) {
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (valor.isArray()) {
            java.util.List<String> lineas = new java.util.ArrayList<>();
            for (tools.jackson.databind.JsonNode elemento : valor) {
                lineas.add(comoTexto(elemento));
            }
            return String.join("\n", lineas);
        }
        if (valor.isObject()) {
            java.util.List<String> partes = new java.util.ArrayList<>();
            for (tools.jackson.databind.JsonNode parte : valor.values()) {
                partes.add(comoTexto(parte));
            }
            return String.join(" - ", partes);
        }
        return valor.asText();
    }

    // "Pedir otra versión": un kit nuevo con los mismos datos con que se pidió este (nombre, rubro, estilo,
    // colores sugeridos, valores, sentimiento y diferencial). Así funciona también al abrir un kit guardado,
    // cuando el formulario de la web está vacío. El eslogan no se repite: la IA propone 3 nuevos.
    public KitMarcaGeneradoResponse otraVersion(Long usuarioId, Long kitId) {
        KitMarca kit = kits.findById(kitId)
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "KIT_MARCA_NO_ENCONTRADO", "Kit de Marca no encontrado: " + kitId));
        return generar(usuarioId, new GenerarKitMarcaRequest(kit.getNombreNegocio(), kit.getSector(),
                kit.getPaletaSugerida(), kit.getValoresEslogan(),
                kit.getEstiloVisual() == null || kit.getEstiloVisual().isBlank() ? "MODERNO" : kit.getEstiloVisual(),
                kit.getSentimiento(), kit.getDiferencial(), null));
    }

    @Transactional(readOnly = true)
    public KitMarcaGeneradoResponse obtener(Long usuarioId, Long kitId) {
        // HU 32: el kit de otra empresa responde 404 igual que uno que no existe (antes daba 500).
        KitMarca kit = kits.findById(kitId)
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "KIT_MARCA_NO_ENCONTRADO", "Kit de Marca no encontrado: " + kitId));
        return KitMarcaGeneradoResponse.desde(kit, false);
    }

    @Transactional(readOnly = true)
    public List<KitMarcaGeneradoResponse> listarPorUsuario(Long usuarioId) {
        return kits.findByUsuarioId(usuarioId).stream()
                .map(k -> KitMarcaGeneradoResponse.desde(k, false))
                .toList();
    }
}