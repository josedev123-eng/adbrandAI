package com.adbrand.core.notificacion.dto;

// HU 51: cambio parcial de preferencias. Un campo nulo deja el valor como estaba,
// así la app puede enviar solo el interruptor que tocó el usuario.
public record PreferenciaNotificacionRequest(
        Boolean campanas,
        Boolean suscripciones,
        Boolean novedades) {
}
