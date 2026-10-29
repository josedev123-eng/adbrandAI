package com.adbrand.core.brandkit.service;

import com.adbrand.core.brandkit.dto.EditarColoresRequest;
import com.adbrand.core.brandkit.dto.EditarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.EditarKitMarcaRequest.ColorKit;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.dto.KitMarcaVersionResponse;
import com.adbrand.core.brandkit.dto.LogoDiseno;
import com.adbrand.core.brandkit.dto.LogoDisenoJson;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.repository.KitMarcaVersionRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 32: el dueño edita a mano colores, tipografías y logotipo del kit. Cada edición queda como versión.
@Service
public class EdicionKitMarcaService {

    private final KitMarcaRepository kits;
    private final KitMarcaVersionRepository versiones;

    public EdicionKitMarcaService(KitMarcaRepository kits, KitMarcaVersionRepository versiones) {
        this.kits = kits;
        this.versiones = versiones;
    }

    @Transactional
    public KitMarcaGeneradoResponse editar(Long usuarioId, Long kitId, EditarKitMarcaRequest datos) {
        KitMarca kit = buscarDelUsuario(usuarioId, kitId);
        String paleta = paletaComoTexto(datos.colores());
        String secundaria = datos.tipografiaSecundaria() == null || datos.tipografiaSecundaria().isBlank()
                ? null : datos.tipografiaSecundaria();

        KitMarcaVersion version = new KitMarcaVersion();
        version.setKitMarcaId(kit.getId());
        version.setPaletaColores(paleta);
        version.setTipografiaPrimaria(datos.tipografiaPrimaria());
        version.setTipografiaSecundaria(secundaria);
        version.setLogoConcepto(datos.logoConcepto());
        // HU 29: la versión también guarda el logo diseñado que el kit tenía en ese momento.
        version.setLogoDiseno(kit.getLogoDiseno());
        versiones.save(version);

        // El kit queda con lo último guardado; @PreUpdate cambia fecha_actualizacion.
        kit.setPaletaColores(paleta);
        kit.setTipografiaPrimaria(datos.tipografiaPrimaria());
        kit.setTipografiaSecundaria(secundaria);
        kit.setLogoConcepto(datos.logoConcepto());
        return KitMarcaGeneradoResponse.desde(kits.saveAndFlush(kit), false);
    }

    // HU 29: guarda el logo diseñado en el editor. Queda como una versión más del kit, con el resto de datos tal cual.
    @Transactional
    public KitMarcaGeneradoResponse guardarLogo(Long usuarioId, Long kitId, LogoDiseno logo) {
        KitMarca kit = buscarDelUsuario(usuarioId, kitId);
        kit.setLogoDiseno(LogoDisenoJson.aTexto(logo));
        return guardarComoVersion(kit);
    }

    // HU 30: guarda solo la paleta elegida en el editor de colores, como una versión nueva del kit.
    @Transactional
    public KitMarcaGeneradoResponse guardarColores(Long usuarioId, Long kitId, EditarColoresRequest datos) {
        KitMarca kit = buscarDelUsuario(usuarioId, kitId);
        sinColoresRepetidos(datos.colores());
        kit.setPaletaColores(paletaComoTexto(datos.colores()));
        return guardarComoVersion(kit);
    }

    // Deja en el historial una foto del kit tal como queda y lo guarda.
    private KitMarcaGeneradoResponse guardarComoVersion(KitMarca kit) {
        KitMarcaVersion version = new KitMarcaVersion();
        version.setKitMarcaId(kit.getId());
        version.setPaletaColores(kit.getPaletaColores());
        version.setTipografiaPrimaria(kit.getTipografiaPrimaria());
        version.setTipografiaSecundaria(kit.getTipografiaSecundaria());
        version.setLogoConcepto(kit.getLogoConcepto());
        version.setLogoDiseno(kit.getLogoDiseno());
        versiones.save(version);
        return KitMarcaGeneradoResponse.desde(kits.saveAndFlush(kit), false);
    }

    // HU 30: el mismo color dos veces no aporta nada a la paleta (#c62828 y #C62828 cuentan como iguales).
    private static void sinColoresRepetidos(List<ColorKit> colores) {
        Set<String> vistos = new HashSet<>();
        for (ColorKit color : colores) {
            String hex = color.hex().toUpperCase(Locale.ROOT);
            if (!vistos.add(hex)) {
                throw new SolicitudInvalidaException("COLOR_REPETIDO", "El color " + hex + " está repetido.");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<KitMarcaVersionResponse> listarVersiones(Long usuarioId, Long kitId) {
        KitMarca kit = buscarDelUsuario(usuarioId, kitId);
        return versiones.findByKitMarcaIdOrderByFechaCreacionDescIdDesc(kit.getId()).stream()
                .map(KitMarcaVersionResponse::desde)
                .toList();
    }

    // Un kit de otra empresa responde igual que uno que no existe: 404.
    private KitMarca buscarDelUsuario(Long usuarioId, Long kitId) {
        return kits.findById(kitId)
                .filter(kit -> kit.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "KIT_MARCA_NO_ENCONTRADO", "Kit de Marca no encontrado: " + kitId));
    }

    // Mismo formato que devuelve la IA en la HU 11: una línea por color, "#RRGGBB - nombre".
    private String paletaComoTexto(List<ColorKit> colores) {
        return colores.stream()
                .map(color -> {
                    String hex = color.hex().toUpperCase(Locale.ROOT);
                    String nombre = color.nombre() == null ? "" : color.nombre().trim();
                    return nombre.isEmpty() ? hex : hex + " - " + nombre;
                })
                .collect(Collectors.joining("\n"));
    }
}
