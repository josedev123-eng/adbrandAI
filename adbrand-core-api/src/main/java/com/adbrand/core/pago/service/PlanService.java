package com.adbrand.core.pago.service;

import com.adbrand.core.pago.dto.PlanResponse;
import com.adbrand.core.pago.repository.PlanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 25: los planes se leen de la tabla plan y se muestran del más barato al más caro.
@Service
public class PlanService {

    private final PlanRepository repositorio;

    public PlanService(PlanRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listar() {
        return repositorio.findByActivoTrueOrderByPrecioAsc().stream()
                .map(PlanResponse::desde)
                .toList();
    }
}
