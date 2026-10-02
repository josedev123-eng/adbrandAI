package com.adbrand.core.ia.service;

// Se lanza cuando el servidor de IA no responde, tarda demasiado o devuelve algo vacío.
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException(String detalle, Throwable causa) {
        super(detalle, causa);
    }
}