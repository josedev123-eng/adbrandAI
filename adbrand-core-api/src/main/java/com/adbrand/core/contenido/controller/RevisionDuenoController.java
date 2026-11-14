package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.ContenidoPorRevisarResponse;
import com.adbrand.core.contenido.service.RevisionDuenoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 53: lista "por revisar" del móvil y botón Aprobar.
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
}
