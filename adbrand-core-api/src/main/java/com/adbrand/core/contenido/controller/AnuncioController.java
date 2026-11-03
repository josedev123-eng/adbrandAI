package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.service.AnuncioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 22: el usuario es el que inició sesión; FiltroJwt deja su id en "usuarioId".
@RestController
@RequestMapping("/api/contenido/anuncios")
public class AnuncioController {

    private final AnuncioService servicio;

    public AnuncioController(AnuncioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public AnuncioGeneradoResponse generar(@RequestAttribute("usuarioId") Long usuarioId,
                                           @Valid @RequestBody GenerarAnuncioRequest datos) {
        return servicio.generar(usuarioId, datos);
    }

    // HU 12: regenerar contenido con los mismos parámetros
    @PostMapping("/{id}/regenerar")
    public AnuncioGeneradoResponse regenerar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.regenerar(usuarioId, id);
    }
}
