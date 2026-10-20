package com.adbrand.core.brandkit.controller;

import com.adbrand.core.brandkit.dto.EditarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.dto.KitMarcaVersionResponse;
import com.adbrand.core.brandkit.service.EdicionKitMarcaService;
import com.adbrand.core.brandkit.service.KitMarcaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// HU 22: el usuario es el que inició sesión; FiltroJwt deja su id en "usuarioId".
@RestController
@RequestMapping("/api/kit-marca")
public class KitMarcaController {

    private final KitMarcaService servicio;
    private final EdicionKitMarcaService edicion;

    public KitMarcaController(KitMarcaService servicio, EdicionKitMarcaService edicion) {
        this.servicio = servicio;
        this.edicion = edicion;
    }

    @PostMapping("/generar")
    public KitMarcaGeneradoResponse generar(@RequestAttribute("usuarioId") Long usuarioId,
                                            @Valid @RequestBody GenerarKitMarcaRequest datos) {
        return servicio.generar(usuarioId, datos);
    }

    @GetMapping("/{id}")
    public KitMarcaGeneradoResponse obtener(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return servicio.obtener(usuarioId, id);
    }

    @GetMapping
    public List<KitMarcaGeneradoResponse> listar(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.listarPorUsuario(usuarioId);
    }

    // HU 32: guarda los cambios hechos a mano y deja una versión en el historial.
    @PutMapping("/{id}")
    public KitMarcaGeneradoResponse editar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                           @Valid @RequestBody EditarKitMarcaRequest datos) {
        return edicion.editar(usuarioId, id, datos);
    }

    // HU 32: historial de ediciones, la más reciente primero.
    @GetMapping("/{id}/versiones")
    public List<KitMarcaVersionResponse> versiones(@RequestAttribute("usuarioId") Long usuarioId,
                                                   @PathVariable Long id) {
        return edicion.listarVersiones(usuarioId, id);
    }
}
