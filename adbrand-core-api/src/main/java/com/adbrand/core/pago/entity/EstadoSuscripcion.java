package com.adbrand.core.pago.entity;

// Estados de una suscripción, igual que el CHECK de la tabla suscripcion (V7).
public enum EstadoSuscripcion {
    ACTIVA,
    VENCIDA,
    PENDIENTE_PAGO,
    CANCELADA
}
