package com.adbrand.core.auth.repository;

import com.adbrand.core.auth.entity.CodigoRecuperacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: los códigos de recuperación que un usuario todavía no usó.
public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    List<CodigoRecuperacion> findByUsuarioIdAndUsadoFalse(Long usuarioId);
}
