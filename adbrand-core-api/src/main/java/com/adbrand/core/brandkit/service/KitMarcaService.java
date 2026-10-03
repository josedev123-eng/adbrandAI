package com.adbrand.core.brandkit.service;

import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.dto.KitMarcaPrompt;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
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
        String respuestaJson = ia.generarTexto(prompt.sistema(), prompt.usuario());

        // Parsear JSON de la IA
        KitMarca kit = parsearYGuardar(usuarioId, datos, respuestaJson);

        return KitMarcaGeneradoResponse.desde(kit, ia.esSimulado());
    }

    private KitMarca parsearYGuardar(Long usuarioId, GenerarKitMarcaRequest datos, String json) {
        // La IA debería devolver JSON puro. En modo simulado, el JSON ya viene bien formado.
        // Usamos Jackson manualmente para evitar dependencia extra en el DTO.
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);

            KitMarca kit = new KitMarca();
            kit.setUsuarioId(usuarioId);
            kit.setNombreNegocio(datos.nombreNegocio());
            kit.setSector(datos.sector());
            kit.setPaletaSugerida(datos.paletaSugerida());
            kit.setValoresEslogan(datos.valoresEslogan());
            kit.setEstiloVisual(datos.estiloVisual());
            kit.setLogoConcepto(node.get("logo_concepto").asText());
            kit.setTipografiaPrimaria(node.get("tipografia_primaria").asText());
            kit.setTipografiaSecundaria(node.get("tipografia_secundaria").asText());
            kit.setPaletaColores(node.get("paleta_colores").asText());
            kit.setVozMarca(node.get("voz_marca").asText());

            return kits.save(kit);
        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta de IA para Kit de Marca: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public KitMarcaGeneradoResponse obtener(Long usuarioId, Long kitId) {
        KitMarca kit = kits.findById(kitId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "KIT_MARCA_NO_ENCONTRADO", "Kit de Marca no encontrado: " + kitId));
        if (!kit.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("El Kit de Marca no pertenece al usuario");
        }
        return KitMarcaGeneradoResponse.desde(kit, false);
    }

    @Transactional(readOnly = true)
    public List<KitMarcaGeneradoResponse> listarPorUsuario(Long usuarioId) {
        return kits.findByUsuarioId(usuarioId).stream()
                .map(k -> KitMarcaGeneradoResponse.desde(k, false))
                .toList();
    }
}