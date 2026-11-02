package com.adbrand.core.previsualizacion.service;

import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.previsualizacion.dto.AnuncioPrevisualizacion;
import com.adbrand.core.previsualizacion.dto.PrevisualizacionResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 45: junta el kit de marca más reciente con los últimos anuncios de la empresa.
@Service
public class PrevisualizacionService {

    // La app muestra una lista corta; no tiene sentido mandar todo el historial.
    static final int MAX_ANUNCIOS = 20;

    private final KitMarcaRepository kits;
    private final ContenidoRepository contenidos;

    public PrevisualizacionService(KitMarcaRepository kits, ContenidoRepository contenidos) {
        this.kits = kits;
        this.contenidos = contenidos;
    }

    @Transactional(readOnly = true)
    public PrevisualizacionResponse obtener(Long usuarioId) {
        KitMarcaGeneradoResponse kit = kits
                .findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(usuarioId)
                .map(encontrado -> KitMarcaGeneradoResponse.desde(encontrado, false))
                .orElse(null);

        List<AnuncioPrevisualizacion> anuncios = contenidos
                .findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuarioId)
                .stream()
                .limit(MAX_ANUNCIOS)
                .map(AnuncioPrevisualizacion::desde)
                .toList();

        return new PrevisualizacionResponse(kit, anuncios);
    }
}
