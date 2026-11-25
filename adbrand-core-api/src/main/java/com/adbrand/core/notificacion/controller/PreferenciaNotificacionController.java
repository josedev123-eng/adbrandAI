package com.adbrand.core.notificacion.controller;

import com.adbrand.core.notificacion.dto.PreferenciaNotificacionRequest;
import com.adbrand.core.notificacion.dto.PreferenciaNotificacionResponse;
import com.adbrand.core.notificacion.service.ServicioPreferenciasNotificacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// HU 51: la app consulta y cambia las preferencias de avisos de la empresa.
@RestController
@RequestMapping("/api/notificaciones/preferencias")
public class PreferenciaNotificacionController {

    private final ServicioPreferenciasNotificacion servicio;

    public PreferenciaNotificacionController(ServicioPreferenciasNotificacion servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PreferenciaNotificacionResponse obtener(@RequestAttribute("usuarioId") Long usuarioId) {
        return servicio.obtener(usuarioId);
    }

    @PatchMapping
    public PreferenciaNotificacionResponse actualizar(
            @RequestAttribute("usuarioId") Long usuarioId,
            @RequestBody PreferenciaNotificacionRequest cambios) {
        return servicio.actualizar(usuarioId, cambios);
    }
}
