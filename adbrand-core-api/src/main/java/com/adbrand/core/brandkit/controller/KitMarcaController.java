package com.adbrand.core.brandkit.controller;

import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.service.KitMarcaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kit-marca")
public class KitMarcaController {

    // Temporal: hasta que exista el inicio de sesión (HU 21), se usa el usuario 1.
    private static final Long USUARIO_DE_PRUEBA = 1L;

    private final KitMarcaService servicio;

    public KitMarcaController(KitMarcaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/generar")
    public KitMarcaGeneradoResponse generar(@Valid @RequestBody GenerarKitMarcaRequest datos) {
        return servicio.generar(USUARIO_DE_PRUEBA, datos);
    }

    @GetMapping("/{id}")
    public KitMarcaGeneradoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(USUARIO_DE_PRUEBA, id);
    }

    @GetMapping
    public List<KitMarcaGeneradoResponse> listar() {
        return servicio.listarPorUsuario(USUARIO_DE_PRUEBA);
    }
}