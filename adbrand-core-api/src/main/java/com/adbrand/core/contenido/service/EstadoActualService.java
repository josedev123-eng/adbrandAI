package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.EstadoActualResponse;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 56: el estado actual de los contenidos de la empresa, para la lista del móvil.
@Service
public class EstadoActualService {

    // Los más recientes; suficiente para la pantalla del celular.
    static final int MAXIMO = 30;

    private final ContenidoRepository contenidos;

    public EstadoActualService(ContenidoRepository contenidos) {
        this.contenidos = contenidos;
    }

    // Primero el último que cambió: lo que se acaba de aprobar o rechazar sale arriba (criterio 1).
    @Transactional(readOnly = true)
    public List<EstadoActualResponse> estados(Long usuarioId) {
        return contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuarioId)
                .stream()
                .filter(contenido -> !contenido.isEliminado())
                .sorted(Comparator.comparing(Contenido::getFechaActualizacion)
                        .thenComparing(Contenido::getId)
                        .reversed())
                .limit(MAXIMO)
                .map(EstadoActualResponse::desde)
                .toList();
    }
}
