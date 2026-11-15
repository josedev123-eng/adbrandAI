package com.adbrand.core.ia.dto;

// Cuánto usó la empresa de su límite de tokens este mes (tarjeta de Inicio y mensaje al llegar al límite).
// origen: PLAN (su plan pagado), GRATUITO (sin plan pagado) o PERSONALIZADO (límite que puso el administrador).
public record ConsumoMesResponse(
        long tokensUsados,
        long limite,
        int porcentaje,
        String origen,
        String plan,
        boolean enAlerta,
        boolean agotado) {
}
