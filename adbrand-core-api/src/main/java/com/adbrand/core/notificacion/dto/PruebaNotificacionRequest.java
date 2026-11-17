package com.adbrand.core.notificacion.dto;

// HU 50: petición del aviso de prueba. Permite elegir el tipo de mensaje para
// validar en el teléfono los tres canales (sistema, campaña y suscripción).
public record PruebaNotificacionRequest(String tipo, Long campanaId) {
}
