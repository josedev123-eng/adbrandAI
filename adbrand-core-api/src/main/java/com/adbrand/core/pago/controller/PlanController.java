package com.adbrand.core.pago.controller;

import com.adbrand.core.pago.dto.PlanResponse;
import com.adbrand.core.pago.service.PlanService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 25: lista de planes de suscripción. Es una ruta pública (no pide token).
@RestController
@RequestMapping("/api/planes")
public class PlanController {

    private final PlanService servicio;

    public PlanController(PlanService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PlanResponse> listar() {
        return servicio.listar();
    }
}
