package com.adbrand.core.negocio.controller;

import com.adbrand.core.negocio.dto.PerfilNegocioRequest;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
@RestController
@RequestMapping("/api/negocio/perfil")
public class PerfilNegocioController {

    // Temporal: hasta que exista el inicio de sesión (HU 21), todo se guarda para el usuario 1.
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final PerfilNegocioService servicio;

    public PerfilNegocioController(PerfilNegocioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PerfilNegocioResponse obtener() {
        return servicio.obtener(USUARIO_DE_PRUEBA);
    }

    @PutMapping
    public PerfilNegocioResponse guardar(@Valid @RequestBody PerfilNegocioRequest datos) {
        return servicio.guardar(USUARIO_DE_PRUEBA, datos);
    }
}