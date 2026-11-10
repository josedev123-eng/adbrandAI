package com.adbrand.core.calendario.repository;

import com.adbrand.core.calendario.entity.Campana;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanaRepository extends JpaRepository<Campana, Long> {

    // HU 35: campañas de la empresa, de la que empieza antes a la que empieza después.
    List<Campana> findByUsuarioIdOrderByFechaInicioAscIdAsc(Long usuarioId);

    // HU 35: una campaña solo se usa si es de la empresa que inició sesión.
    Optional<Campana> findByIdAndUsuarioId(Long id, Long usuarioId);
}
