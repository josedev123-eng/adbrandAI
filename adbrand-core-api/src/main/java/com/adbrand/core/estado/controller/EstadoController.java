package com.adbrand.core.estado.controller;

import com.adbrand.core.estado.dto.EstadoResponse;
import com.adbrand.core.estado.service.EstadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 18: Django pregunta aquí si el backend y la IA están activos.
// Cuando llegue el login de usuarios (FiltroJwt, HU 21) esta ruta debe quedar pública.
@RestController
@RequestMapping("/api/estado")
public class EstadoController {

    private final EstadoService servicio;

    public EstadoController(EstadoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public EstadoResponse estado() {
        return servicio.verificar();
    }
}
