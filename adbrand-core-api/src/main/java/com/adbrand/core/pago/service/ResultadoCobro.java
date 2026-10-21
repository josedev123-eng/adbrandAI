package com.adbrand.core.pago.service;

// Resultado de cobrar en la pasarela. Con aprobado=false, motivo explica por qué se rechazó.
public record ResultadoCobro(boolean aprobado, String referencia, String motivo) {

    public static ResultadoCobro aprobado(String referencia) {
        return new ResultadoCobro(true, referencia, null);
    }

    public static ResultadoCobro rechazado(String motivo) {
        return new ResultadoCobro(false, null, motivo);
    }
}
