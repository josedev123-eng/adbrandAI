package com.adbrand.core.pago.repository;

import com.adbrand.core.pago.entity.Plan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActivoTrueOrderByPrecioAsc();

    // Límite de tokens: la suscripción guarda el nombre del plan.
    Optional<Plan> findFirstByNombre(String nombre);
}
