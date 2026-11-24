package com.adbrand.core.pago.service;

// Resultado del cobro en Mercado Pago, con el titular y los últimos 4 dígitos que devuelve el pago
// (el formulario de Mercado Pago no los entrega a la web). Se usan en el comprobante (HU 28).
public record CobroMercadoPago(ResultadoCobro resultado, String titular, String ultimosDigitos) {

    public static CobroMercadoPago sinTarjeta(ResultadoCobro resultado) {
        return new CobroMercadoPago(resultado, null, null);
    }
}
