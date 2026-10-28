package com.adbrand.core.reporte.controller;

import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.service.InformeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 37: no hay id en la URL; el informe es de la empresa que inició sesión ("usuarioId" de FiltroJwt).
@RestController
@RequestMapping("/api/reportes")
public class InformeController {

    private final InformeService servicio;

    public InformeController(InformeService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/campanas")
    public InformeCampanasResponse campanas(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.campanas(usuarioId);
    }
}
