package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.ContenidoPorRevisarResponse;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.DecisionDueno;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 53 y 54: el dueño revisa desde el móvil el contenido generado y lo aprueba o lo rechaza.
@Service
public class RevisionDuenoService {

    private final ContenidoRepository contenidos;
    private final PublicacionProgramadaRepository publicaciones;

    public RevisionDuenoService(ContenidoRepository contenidos, PublicacionProgramadaRepository publicaciones) {
        this.contenidos = contenidos;
        this.publicaciones = publicaciones;
    }

    // Lista "por revisar": lo que el dueño todavía no aprobó ni rechazó, del más reciente al más antiguo.
    @Transactional(readOnly = true)
    public List<ContenidoPorRevisarResponse> porRevisar(Long usuarioId) {
        return contenidos.findByUsuarioIdAndDecisionDuenoOrderByFechaCreacionDescIdDesc(usuarioId, DecisionDueno.PENDIENTE)
                .stream()
                .map(ContenidoPorRevisarResponse::desde)
                .toList();
    }

    // Criterio 1: pasa a aprobado y queda listo para programarlo en el calendario (HU 34).
    // Criterio 2: solo el de la propia empresa; lo rechazado por el moderador no se aprueba.
    @Transactional
    public ContenidoPorRevisarResponse aprobar(Long usuarioId, Long contenidoId) {
        Contenido contenido = propio(usuarioId, contenidoId);

        if (contenido.getEstado() == EstadoContenido.RECHAZADO) {
            throw new SolicitudInvalidaException("CONTENIDO_RECHAZADO",
                    "Este contenido fue rechazado por el moderador y no se puede aprobar.");
        }
        if (contenido.getEstado() == EstadoContenido.DUDOSO) {
            throw new SolicitudInvalidaException("CONTENIDO_EN_REVISION",
                    "Este contenido todavía lo está revisando un moderador.");
        }
        if (contenido.getDecisionDueno() == DecisionDueno.RECHAZADO) {
            throw new SolicitudInvalidaException("CONTENIDO_YA_RECHAZADO", "Ya rechazaste este contenido.");
        }
        if (contenido.getDecisionDueno() != DecisionDueno.APROBADO) {
            contenido.setDecisionDueno(DecisionDueno.APROBADO);
            contenido.setFechaDecision(LocalDateTime.now());
        }
        return ContenidoPorRevisarResponse.desde(contenidos.save(contenido));
    }

    // HU 54, criterio 1: el contenido rechazado sale de la lista "por revisar", con un motivo opcional,
    // y ya no se puede programar en el calendario (CalendarioService lo revisa).
    // Criterio 2: si ya está programado en el calendario, primero hay que quitarlo de ahí.
    @Transactional
    public ContenidoPorRevisarResponse rechazar(Long usuarioId, Long contenidoId, String motivo) {
        Contenido contenido = propio(usuarioId, contenidoId);

        if (publicaciones.existsByContenidoIdAndEstado(contenidoId, EstadoPublicacion.PROGRAMADA)) {
            throw new SolicitudInvalidaException("CONTENIDO_PROGRAMADO",
                    "Este contenido está programado en tu calendario. Quítalo del calendario antes de rechazarlo.");
        }
        contenido.setDecisionDueno(DecisionDueno.RECHAZADO);
        contenido.setMotivoDueno(motivo);
        contenido.setFechaDecision(LocalDateTime.now());
        return ContenidoPorRevisarResponse.desde(contenidos.save(contenido));
    }

    // El contenido de otra empresa se trata como inexistente, igual que en el calendario.
    private Contenido propio(Long usuarioId, Long contenidoId) {
        return contenidos.findById(contenidoId)
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("CONTENIDO_NO_ENCONTRADO",
                        "El contenido no existe."));
    }
}
