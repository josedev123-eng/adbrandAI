package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.EstadoActualResponse;
import com.adbrand.core.contenido.service.EstadoActualService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 56: GET /api/contenido/estados, la lista del móvil con el estado nuevo de cada contenido.
@RestController
@RequestMapping("/api/contenido")
public class EstadoActualController {

    private final EstadoActualService servicio;

    public EstadoActualController(EstadoActualService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/estados")
    public List<EstadoActualResponse> estados(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.estados(usuarioId);
    }
}
