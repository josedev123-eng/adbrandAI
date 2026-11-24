package com.adbrand.core.contenido.controller;

import com.adbrand.core.contenido.dto.MiAnuncioResponse;
import com.adbrand.core.contenido.service.MisAnunciosService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// "Mis anuncios" y los avisos de lo que decidió el moderador.
@RestController
@RequestMapping("/api/contenido")
public class MisAnunciosController {

    private final MisAnunciosService servicio;

    public MisAnunciosController(MisAnunciosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mis-anuncios")
    public List<MiAnuncioResponse> misAnuncios(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.listar(usuarioId);
    }

    @GetMapping("/avisos")
    public Map<String, Long> avisos(@RequestAttribute("usuarioId") Long usuarioId) {
        return Map.of("pendientes", servicio.avisosPendientes(usuarioId));
    }

    @PostMapping("/avisos/vistos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarVistos(@RequestAttribute("usuarioId") Long usuarioId) {
        servicio.marcarVistos(usuarioId);
    }
}
