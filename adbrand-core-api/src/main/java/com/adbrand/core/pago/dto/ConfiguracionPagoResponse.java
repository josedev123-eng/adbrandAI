package com.adbrand.core.pago.dto;

// HU 26: la web pregunta qué pasarela usar. Con "mercadopago" dibuja el formulario de Mercado Pago
// con la llave pública; con "simulada" usa el formulario de siempre.
public record ConfiguracionPagoResponse(String pasarela, String publicKey) {

    public static final String MERCADO_PAGO = "mercadopago";
    public static final String SIMULADA = "simulada";
}
