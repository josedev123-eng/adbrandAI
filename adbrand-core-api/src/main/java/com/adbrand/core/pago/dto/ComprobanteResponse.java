package com.adbrand.core.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Comprobante de un pago (HU 28). La web lo muestra después de confirmar el cobro.
public record ComprobanteResponse(
        String numeroComprobante,
        Long pagoId,
        String cliente,
        String correo,
        String plan,
        BigDecimal monto,
        String metodo,
        String referencia,
        LocalDate fechaPago,
        boolean correoEnviado) {
}
