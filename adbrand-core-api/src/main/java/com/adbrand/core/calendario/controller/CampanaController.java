package com.adbrand.core.calendario.controller;

import com.adbrand.core.calendario.dto.CampanaResponse;
import com.adbrand.core.calendario.dto.CrearCampanaRequest;
import com.adbrand.core.calendario.service.CampanaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 35: las campañas son de la empresa que inició sesión ("usuarioId" de FiltroJwt).
@RestController
@RequestMapping("/api/campanas")
public class CampanaController {

    private final CampanaService servicio;

    public CampanaController(CampanaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CampanaResponse> listar(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.listar(usuarioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampanaResponse crear(@RequestAttribute("usuarioId") Long usuarioId,
                                 @Valid @RequestBody CrearCampanaRequest datos) {
        return servicio.crear(usuarioId, datos);
    }
}
