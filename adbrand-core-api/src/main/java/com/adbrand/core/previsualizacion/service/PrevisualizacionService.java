package com.adbrand.core.previsualizacion.service;

import com.adbrand.core.contenido.entity.VersionAnuncio;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.contenido.repository.VersionAnuncioRepository;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import com.adbrand.core.previsualizacion.dto.ImagenAnuncioResponse;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
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
    private final OpcionAnuncioRepository opciones;
    private final VersionAnuncioRepository versiones;

    public PrevisualizacionService(KitMarcaRepository kits, ContenidoRepository contenidos,
                                   OpcionAnuncioRepository opciones, VersionAnuncioRepository versiones) {
        this.kits = kits;
        this.contenidos = contenidos;
        this.opciones = opciones;
        this.versiones = versiones;
    }

    // HU 46: imagen actual del anuncio (la versión retocada que se está usando o la original de la opción elegida).
    // Solo de la propia empresa; un anuncio sin imagen (por ejemplo, de la HU 10, solo texto) devuelve imagen null.
    @Transactional(readOnly = true)
    public ImagenAnuncioResponse imagen(Long usuarioId, Long contenidoId) {
        contenidos.findById(contenidoId)
                .filter(contenido -> contenido.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("CONTENIDO_NO_ENCONTRADO", "El anuncio no existe."));

        return opciones.findFirstByContenidoIdAndElegidaTrue(contenidoId)
                .map(opcion -> {
                    ImagenGenerada actual = opcion.getVersionActual() == null
                            ? opcion.imagen()
                            : versiones.findByOpcionIdAndNumero(opcion.getId(), opcion.getVersionActual())
                                    .map(VersionAnuncio::imagen)
                                    .orElse(opcion.imagen());
                    return new ImagenAnuncioResponse(contenidoId, actual.base64() == null ? null : actual.dataUrl());
                })
                .orElse(new ImagenAnuncioResponse(contenidoId, null));
    }

    @Transactional(readOnly = true)
    public PrevisualizacionResponse obtener(Long usuarioId) {
        return obtener(usuarioId, FormatoAnuncio.CUADRADO);
    }

    // HU 46: los anuncios ajustados al formato elegido (el texto que no entra se acorta con "…").
    @Transactional(readOnly = true)
    public PrevisualizacionResponse obtener(Long usuarioId, FormatoAnuncio formato) {
        KitMarcaGeneradoResponse kit = kits
                .findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(usuarioId)
                .map(encontrado -> KitMarcaGeneradoResponse.desde(encontrado, false))
                .orElse(null);

        List<AnuncioPrevisualizacion> anuncios = contenidos
                .findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuarioId)
                .stream()
                .limit(MAX_ANUNCIOS)
                .map(contenido -> AnuncioPrevisualizacion.desde(contenido, formato))
                .toList();

        return new PrevisualizacionResponse(PrevisualizacionResponse.FormatoResponse.desde(formato), kit, anuncios);
    }
}
