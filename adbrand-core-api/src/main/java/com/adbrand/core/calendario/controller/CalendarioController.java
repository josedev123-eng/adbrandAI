package com.adbrand.core.calendario.controller;

import com.adbrand.core.calendario.dto.CrearPublicacionRequest;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.service.CalendarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// HU 33: calendario mensual de publicaciones. Como los demás controllers, no tiene lógica.
@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    // Se usa solo si la petición llega sin el filtro de sesión (pruebas o arranque local).
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final CalendarioService servicio;

    public CalendarioController(CalendarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PublicacionResponse> listar(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @RequestParam(required = false) String mes) {
        return servicio.listar(usuario(usuarioId), mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse crear(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @Valid @RequestBody CrearPublicacionRequest datos) {
        return servicio.crear(usuario(usuarioId), datos);
    }

    private Long usuario(Long usuarioId) {
        return usuarioId == null ? USUARIO_DE_PRUEBA : usuarioId;
    }
}
