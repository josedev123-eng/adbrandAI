package com.adbrand.core.notificacion.controller;

import com.adbrand.core.notificacion.dto.DispositivoResponse;
import com.adbrand.core.notificacion.dto.RegistrarDispositivoRequest;
import com.adbrand.core.notificacion.service.ServicioDispositivos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// HU 50: la app registra su token al iniciar sesión y lo da de baja al cerrar.
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ServicioDispositivos dispositivos;

    public NotificacionController(ServicioDispositivos dispositivos) {
        this.dispositivos = dispositivos;
    }

    @PostMapping("/dispositivos")
    @ResponseStatus(HttpStatus.CREATED)
    public DispositivoResponse registrar(@RequestAttribute("usuarioId") Long usuarioId,
                                         @Valid @RequestBody RegistrarDispositivoRequest datos) {
        return dispositivos.registrar(usuarioId, datos);
    }

    @DeleteMapping("/dispositivos/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable String token) {
        dispositivos.desactivar(usuarioId, token);
    }
}
