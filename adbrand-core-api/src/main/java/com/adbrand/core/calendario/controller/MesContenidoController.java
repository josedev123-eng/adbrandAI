package com.adbrand.core.calendario.controller;

import com.adbrand.core.calendario.dto.MesContenidoResponse;
import com.adbrand.core.calendario.service.MesContenidoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// HU 63: "Tu mes de contenido" de la empresa que inició sesión. Sin anio/mes, el mes actual (hora de Lima).
@RestController
@RequestMapping("/api/calendario/mes-de-contenido")
public class MesContenidoController {

    private final MesContenidoService servicio;

    public MesContenidoController(MesContenidoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public MesContenidoResponse obtener(@RequestAttribute("usuarioId") Long usuarioId,
                                        @RequestParam(required = false) Integer anio,
                                        @RequestParam(required = false) Integer mes) {
        return servicio.generar(usuarioId, anio, mes);
    }
}
