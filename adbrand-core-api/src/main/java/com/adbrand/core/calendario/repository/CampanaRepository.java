package com.adbrand.core.calendario.repository;

import com.adbrand.core.calendario.entity.Campana;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampanaRepository extends JpaRepository<Campana, Long> {

    // HU 35: campañas de la empresa, de la que empieza antes a la que empieza después.
    List<Campana> findByUsuarioIdOrderByFechaInicioAscIdAsc(Long usuarioId);

    // HU 35: una campaña solo se usa si es de la empresa que inició sesión.
    Optional<Campana> findByIdAndUsuarioId(Long id, Long usuarioId);

    // HU 49: campañas cuya fecha de inicio o de fin cae exactamente en el día
    // indicado. Una campaña que empieza y termina el mismo día aparece una sola vez.
    @Query("""
            select c from Campana c
            where c.fechaInicio = :fecha or c.fechaFin = :fecha
            order by c.usuarioId asc, c.id asc
            """)
    List<Campana> buscarConFechaClave(@Param("fecha") LocalDate fecha);
}
