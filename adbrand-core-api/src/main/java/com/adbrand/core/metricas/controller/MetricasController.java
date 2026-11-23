package com.adbrand.core.metricas.controller;

import com.adbrand.core.metricas.dto.MetricasContenidoResponse;
import com.adbrand.core.metricas.service.MetricasContenidoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 57: métricas para la app móvil; la empresa sale del token ("usuarioId" de FiltroJwt), sin id en la URL.
@RestController
@RequestMapping("/api/metricas")
public class MetricasController {

    private final MetricasContenidoService contenido;

    public MetricasController(MetricasContenidoService contenido) {
        this.contenido = contenido;
    }

    @GetMapping("/contenido")
    public MetricasContenidoResponse contenido(@RequestAttribute("usuarioId") Long usuarioId) {
        return contenido.contenido(usuarioId);
    }
}
