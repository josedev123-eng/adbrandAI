package com.adbrand.core.pago.service;

import com.adbrand.core.pago.dto.MiSuscripcionResponse;
import com.adbrand.core.pago.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 52: plan actual de la empresa para la pantalla del móvil.
@Service
public class SuscripcionService {

    private final SuscripcionRepository repositorio;

    public SuscripcionService(SuscripcionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public MiSuscripcionResponse mia(Long usuarioId) {
        return repositorio.findFirstByUsuarioIdOrderByFechaVencimientoDesc(usuarioId)
                .map(MiSuscripcionResponse::desde)
                .orElseGet(MiSuscripcionResponse::sinPlan);
    }
}
