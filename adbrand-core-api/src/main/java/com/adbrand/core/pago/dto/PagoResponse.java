package com.adbrand.core.pago.dto;

import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import java.math.BigDecimal;
import java.time.LocalDate;

// Lo que devuelve un pago aprobado (HU 26). El número de comprobante se agrega en la HU 28.
public record PagoResponse(
        Long id,
        String plan,
        BigDecimal monto,
        String metodo,
        String estado,
        String referencia,
        LocalDate fechaPago) {

    public static PagoResponse desde(Pago pago, Plan plan) {
        return new PagoResponse(pago.getId(), plan.getNombre(), pago.getMonto(),
                pago.getMetodo().name(), pago.getEstado().name(), pago.getReferencia(),
                pago.getFechaPago());
    }
}
