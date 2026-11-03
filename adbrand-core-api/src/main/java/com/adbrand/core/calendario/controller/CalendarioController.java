package com.adbrand.core.calendario.controller;

import com.adbrand.core.calendario.dto.CrearPublicacionRequest;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.dto.ReprogramarRequest;
import com.adbrand.core.calendario.service.CalendarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) Long campanaId) {
        // HU 35: sin campanaId se pide el calendario completo, como en la HU 33.
        return campanaId == null
                ? servicio.listar(usuario(usuarioId), mes)
                : servicio.listar(usuario(usuarioId), mes, campanaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse crear(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @Valid @RequestBody CrearPublicacionRequest datos) {
        return servicio.crear(usuario(usuarioId), datos);
    }

    // HU 36: cambia el día de una publicación al arrastrarla en el calendario.
    @PatchMapping("/{id}/fecha")
    public PublicacionResponse reprogramar(
            @RequestAttribute(value = "usuarioId", required = false) Long usuarioId,
            @PathVariable Long id,
            @Valid @RequestBody ReprogramarRequest datos) {
        return servicio.reprogramar(usuario(usuarioId), id, datos.fecha());
    }

    private Long usuario(Long usuarioId) {
        return usuarioId == null ? USUARIO_DE_PRUEBA : usuarioId;
    }
}
