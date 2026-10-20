package com.adbrand.core.pago.dto;

import com.adbrand.core.pago.entity.Plan;
import java.math.BigDecimal;

// Lo que ve el usuario en la pantalla de planes (HU 25).
public record PlanResponse(Long id, String nombre, BigDecimal precio, Integer limiteTokens, String descripcion) {

    public static PlanResponse desde(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getNombre(), plan.getPrecio(),
                plan.getLimiteTokens(), plan.getDescripcion());
    }
}
