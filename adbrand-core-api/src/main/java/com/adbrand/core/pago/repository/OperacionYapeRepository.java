package com.adbrand.core.pago.repository;

import com.adbrand.core.pago.entity.EstadoOperacionYape;
import com.adbrand.core.pago.entity.OperacionYape;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionYapeRepository extends JpaRepository<OperacionYape, Long> {

    // HU 27: cada usuario solo puede confirmar o anular sus propias operaciones.
    Optional<OperacionYape> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<OperacionYape> findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(
            Long usuarioId, EstadoOperacionYape estado);
}
