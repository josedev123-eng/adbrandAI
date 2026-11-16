package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.ContenidoPorRevisarResponse;
import com.adbrand.core.contenido.dto.RechazarContenidoRequest;
import com.adbrand.core.contenido.service.RevisionDuenoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 53 y 54: lista "por revisar" del móvil y botones Aprobar y Rechazar.
@RestController
@RequestMapping("/api/contenido")
public class RevisionDuenoController {

    private final RevisionDuenoService servicio;

    public RevisionDuenoController(RevisionDuenoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/por-revisar")
    public List<ContenidoPorRevisarResponse> porRevisar(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.porRevisar(usuarioId);
    }

    @PostMapping("/{id}/aprobar")
    public ContenidoPorRevisarResponse aprobar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.aprobar(usuarioId, id);
    }

    // HU 54: el cuerpo es opcional; { "motivo": "..." } si el dueño quiere explicar por qué.
    @PostMapping("/{id}/rechazar")
    public ContenidoPorRevisarResponse rechazar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                                @Valid @RequestBody(required = false) RechazarContenidoRequest datos) {
        return servicio.rechazar(usuarioId, id, datos == null ? null : datos.motivo());
    }
}
