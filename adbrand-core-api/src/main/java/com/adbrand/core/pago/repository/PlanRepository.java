package com.adbrand.core.pago.repository;

import com.adbrand.core.pago.entity.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByActivoTrueOrderByPrecioAsc();
}
