package com.adbrand.core.notificacion.dto;

import com.adbrand.core.notificacion.entity.PreferenciaNotificacion;
import java.time.LocalDateTime;

// HU 51: preferencias de avisos que muestra la pantalla del móvil.
public record PreferenciaNotificacionResponse(
        boolean campanas,
        boolean suscripciones,
        boolean novedades,
        boolean todasActivas,
        LocalDateTime actualizadoEn) {

    public static PreferenciaNotificacionResponse desde(PreferenciaNotificacion preferencia) {
        boolean campanas = preferencia.isCampanas();
        boolean suscripciones = preferencia.isSuscripciones();
        boolean novedades = preferencia.isNovedades();
        return new PreferenciaNotificacionResponse(
                campanas,
                suscripciones,
                novedades,
                campanas && suscripciones && novedades,
                preferencia.getActualizadoEn());
    }
}
