package com.adbrand.core.pago.dto;

import com.adbrand.core.pago.entity.Suscripcion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// HU 52: el plan actual de la empresa, con el modo de renovación y los días que
// faltan para vencer. Sin suscripción se devuelve estado SIN_PLAN y la app
// muestra solo el catálogo de planes.
public record MiSuscripcionResponse(
        Long id,
        String plan,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        boolean renovacionAutomatica,
        long diasRestantes,
        BigDecimal monto) {

    public static MiSuscripcionResponse desde(Suscripcion suscripcion) {
        long dias = suscripcion.getFechaVencimiento() == null
                ? 0
                : ChronoUnit.DAYS.between(LocalDate.now(), suscripcion.getFechaVencimiento());
        return new MiSuscripcionResponse(
                suscripcion.getId(),
                suscripcion.getPlan(),
                suscripcion.getEstado().name(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(),
                suscripcion.isRenovacionAutomatica(),
                dias,
                suscripcion.getMonto());
    }

    public static MiSuscripcionResponse sinPlan() {
        return new MiSuscripcionResponse(null, null, "SIN_PLAN", null, null, false, 0, null);
    }
}
