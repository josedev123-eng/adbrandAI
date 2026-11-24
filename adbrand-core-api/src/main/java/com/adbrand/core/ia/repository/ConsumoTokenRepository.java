package com.adbrand.core.ia.repository;

import com.adbrand.core.ia.entity.ConsumoToken;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repository: Spring guarda el consumo y lo suma para hacer cumplir el límite del plan.
// El panel del administrador técnico (HU 17) lo consulta Django.
public interface ConsumoTokenRepository extends JpaRepository<ConsumoToken, Long> {

    // Tokens que gastó una empresa desde el inicio del mes (usa el índice de V30).
    @Query("select coalesce(sum(c.tokensTotal), 0) from ConsumoToken c where c.usuarioId = :usuarioId and c.fecha >= :desde")
    long sumarTokensDesde(@Param("usuarioId") Long usuarioId, @Param("desde") OffsetDateTime desde);
}
