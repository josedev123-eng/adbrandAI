package com.adbrand.core.metricas.controller;

import com.adbrand.core.metricas.dto.MetricasContenidoResponse;
import com.adbrand.core.metricas.dto.MetricasPublicacionesResponse;
import com.adbrand.core.metricas.service.MetricasContenidoService;
import com.adbrand.core.metricas.service.MetricasPublicacionesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 57 y 58: métricas para la app móvil; la empresa sale del token ("usuarioId" de FiltroJwt), sin id en la URL.
@RestController
@RequestMapping("/api/metricas")
public class MetricasController {

    private final MetricasContenidoService contenido;
    private final MetricasPublicacionesService publicaciones;

    public MetricasController(MetricasContenidoService contenido, MetricasPublicacionesService publicaciones) {
        this.contenido = contenido;
        this.publicaciones = publicaciones;
    }

    // HU 60: desde y hasta (AAAA-MM-DD) son opcionales; sin ellos se cuenta todo.
    @GetMapping("/contenido")
    public MetricasContenidoResponse contenido(@RequestAttribute("usuarioId") Long usuarioId,
                                               @RequestParam(required = false) String desde,
                                               @RequestParam(required = false) String hasta) {
        return contenido.contenido(usuarioId, desde, hasta);
    }

    // HU 58: programadas, publicadas y canceladas, por red social, y las próximas publicaciones.
    @GetMapping("/publicaciones")
    public MetricasPublicacionesResponse publicaciones(@RequestAttribute("usuarioId") Long usuarioId,
                                                       @RequestParam(required = false) String desde,
                                                       @RequestParam(required = false) String hasta) {
        return publicaciones.publicaciones(usuarioId, desde, hasta);
    }
}
