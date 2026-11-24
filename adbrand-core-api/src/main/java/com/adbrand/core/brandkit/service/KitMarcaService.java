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

            return kits.save(kit);
        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta de IA para Kit de Marca: " + e.getMessage(), e);
        }
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