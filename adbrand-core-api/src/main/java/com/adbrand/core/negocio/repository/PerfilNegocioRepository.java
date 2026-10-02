package com.adbrand.core.negocio.repository;

import com.adbrand.core.negocio.entity.PerfilNegocio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: Spring genera el SQL a partir del nombre del método.
public interface PerfilNegocioRepository extends JpaRepository<PerfilNegocio, Long> {

    Optional<PerfilNegocio> findByUsuarioId(Long usuarioId);
}