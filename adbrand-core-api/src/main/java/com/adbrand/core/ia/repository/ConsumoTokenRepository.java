package com.adbrand.core.ia.repository;

import com.adbrand.core.ia.entity.ConsumoToken;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: Spring solo guarda el consumo. El panel del administrador técnico (HU 17) lo consulta Django.
public interface ConsumoTokenRepository extends JpaRepository<ConsumoToken, Long> {
}
