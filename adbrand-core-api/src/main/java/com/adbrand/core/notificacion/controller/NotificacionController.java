package com.adbrand.core.notificacion.controller;

import com.adbrand.core.notificacion.dto.DispositivoResponse;
import com.adbrand.core.notificacion.dto.EnvioPushResponse;
import com.adbrand.core.notificacion.dto.PruebaNotificacionRequest;
import com.adbrand.core.notificacion.dto.RegistrarDispositivoRequest;
import com.adbrand.core.notificacion.entity.TipoNotificacion;
import com.adbrand.core.notificacion.service.ServicioDispositivos;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
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
// El endpoint de prueba sirve para verificar la recepción en el teléfono.
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ServicioDispositivos dispositivos;
    private final ServicioNotificaciones notificaciones;

    public NotificacionController(ServicioDispositivos dispositivos,
                                  ServicioNotificaciones notificaciones) {
        this.dispositivos = dispositivos;
        this.notificaciones = notificaciones;
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

    /**
     * HU 50: aviso de prueba para comprobar la recepción con la app abierta y
     * cerrada. Solo se envía a los equipos del usuario que inició sesión.
     */
    @PostMapping("/prueba")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EnvioPushResponse prueba(@RequestAttribute("usuarioId") Long usuarioId,
                                    @RequestBody(required = false) PruebaNotificacionRequest datos) {
        TipoNotificacion tipo = TipoNotificacion.desde(datos == null ? null : datos.tipo());
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", tipo.valor());
        if (datos != null && datos.campanaId() != null) {
            payload.put("campaign_id", String.valueOf(datos.campanaId()));
        }

        String titulo;
        String cuerpo;
        switch (tipo) {
            case CAMPANA -> {
                titulo = "Mañana empieza tu campaña";
                cuerpo = "Revisa el calendario para no perderte el inicio.";
            }
            case SUSCRIPCION -> {
                titulo = "Tu plan vence pronto";
                cuerpo = "Te quedan 3 días. Renueva a tiempo para seguir publicando.";
            }
            default -> {
                titulo = "Aviso de prueba";
                cuerpo = "Los avisos de AdBrand ya funcionan en este teléfono.";
            }
        }

        int enviados = notificaciones.enviarAUsuario(usuarioId, titulo, cuerpo, payload);
        return new EnvioPushResponse(enviados);
    }
}
