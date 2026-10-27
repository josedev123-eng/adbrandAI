package com.adbrand.core.pago.dto;

import com.adbrand.core.pago.entity.OperacionYape;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Lo que ve la pantalla de Yape (HU 27): los datos para dibujar el QR y el código de la operación.
public record OperacionYapeResponse(
        Long id,
        String codigo,
        String plan,
        BigDecimal monto,
        String estado,
        String celular,
        LocalDateTime fechaCreacion,
        String contenidoQr) {

    public static OperacionYapeResponse desde(OperacionYape operacion, String nombrePlan) {
        return new OperacionYapeResponse(
                operacion.getId(),
                operacion.getCodigo(),
                nombrePlan,
                operacion.getMonto(),
                operacion.getEstado().name(),
                operacion.getCelular(),
                operacion.getFechaCreacion(),
                contenidoQr(operacion));
    }

    // Texto que la web convierte en QR con la librería qrcode. Yape lo leería para completar el cobro.
    private static String contenidoQr(OperacionYape operacion) {
        return "yape://pagar?codigo=" + operacion.getCodigo()
                + "&monto=" + operacion.getMonto().toPlainString()
                + "&moneda=PEN";
    }
}
