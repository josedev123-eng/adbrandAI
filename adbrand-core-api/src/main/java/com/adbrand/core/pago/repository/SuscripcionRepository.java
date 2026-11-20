package com.adbrand.core.pago.repository;

import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.entity.Suscripcion;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    // HU 26: evita cobrar dos veces el mismo plan mientras la suscripción siga vigente.
    boolean existsByUsuarioIdAndPlanAndEstadoAndFechaVencimientoGreaterThanEqual(
            Long usuarioId, String plan, EstadoSuscripcion estado, LocalDate fecha);

    // HU 52: suscripciones activas que vencen dentro de los próximos días.
    List<Suscripcion> findByEstadoAndFechaVencimientoBetweenOrderByUsuarioIdAscFechaVencimientoAsc(
            EstadoSuscripcion estado, LocalDate desde, LocalDate hasta);
}
