package com.adbrand.core.pago.entity;

// Estados de una operación de pago con Yape (columna operacion_yape.estado, V22).
public enum EstadoOperacionYape {
    PENDIENTE,
    CONFIRMADO,
    ANULADO
}
