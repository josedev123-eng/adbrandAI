package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.AnuncioGeneradoResponse;
import com.adbrand.core.contenido.dto.GenerarAnuncioRequest;
import com.adbrand.core.contenido.service.AnuncioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contenido/anuncios")
public class AnuncioController {

    // Temporal: hasta que exista el inicio de sesión (HU 21), se usa el usuario 1.
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final AnuncioService servicio;

    public AnuncioController(AnuncioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public AnuncioGeneradoResponse generar(@Valid @RequestBody GenerarAnuncioRequest datos) {
        return servicio.generar(USUARIO_DE_PRUEBA, datos);
    }

    // HU 12: regenerar contenido con los mismos parámetros
    @PostMapping("/{id}/regenerar")
    public AnuncioGeneradoResponse regenerar(@PathVariable Long id) {
        return servicio.regenerar(USUARIO_DE_PRUEBA, id);
    }
}