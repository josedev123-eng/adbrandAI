package com.adbrand.core.reporte.dto;

import java.util.List;

// HU 37: el informe consolidado de campañas de una empresa, con los totales por estado.
// Si la empresa no tiene campañas, "campanas" viene vacía y la web avisa en lugar de descargar (criterio 2).
public record InformeCampanasResponse(
        String empresa,
        int total,
        long aprobados,
        long dudosos,
        long rechazados,
        List<CampanaInformeResponse> campanas) {
}
