package com.adbrand.core.negocio.controller;

import com.adbrand.core.negocio.dto.PerfilNegocioRequest;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 22: el usuario es el que inició sesión; FiltroJwt deja su id en "usuarioId".
@RestController
@RequestMapping("/api/negocio/perfil")
public class PerfilNegocioController {

    private final PerfilNegocioService servicio;

    public PerfilNegocioController(PerfilNegocioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PerfilNegocioResponse obtener(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.obtener(usuarioId);
    }

    @PutMapping
    public PerfilNegocioResponse guardar(@RequestAttribute("usuarioId") Long usuarioId,
                                         @Valid @RequestBody PerfilNegocioRequest datos) {
        return servicio.guardar(usuarioId, datos);
    }
}
