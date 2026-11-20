package com.adbrand.core.pago.controller;

import com.adbrand.core.pago.dto.MiSuscripcionResponse;
import com.adbrand.core.pago.service.SuscripcionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 52: la app móvil consulta aquí el plan de la empresa que inició sesión.
@RestController
@RequestMapping("/api/suscripciones")
public class SuscripcionController {

    private final SuscripcionService servicio;

    public SuscripcionController(SuscripcionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mia")
    public MiSuscripcionResponse mia(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.mia(usuarioId);
    }
}
