package com.adbrand.core.previsualizacion.controller;

import com.adbrand.core.previsualizacion.dto.PrevisualizacionResponse;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 45: la empresa sale del token; no se recibe un id por la URL.
@RestController
@RequestMapping("/api/previsualizacion")
public class PrevisualizacionController {

    private final PrevisualizacionService servicio;

    public PrevisualizacionController(PrevisualizacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PrevisualizacionResponse obtener(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.obtener(usuarioId);
    }
}
