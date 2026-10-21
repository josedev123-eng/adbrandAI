package com.adbrand.core.pago.repository;

import com.adbrand.core.pago.entity.Pago;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // HU 28: cada usuario solo puede ver el comprobante de sus propios pagos.
    Optional<Pago> findByIdAndUsuarioId(Long id, Long usuarioId);
}
