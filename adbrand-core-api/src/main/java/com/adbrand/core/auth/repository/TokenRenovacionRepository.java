package com.adbrand.core.auth.repository;

import com.adbrand.core.auth.entity.TokenRenovacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// HU 43: búsqueda por hash; el token viaja en claro solo entre la app y Spring.
public interface TokenRenovacionRepository extends JpaRepository<TokenRenovacion, Long> {

    Optional<TokenRenovacion> findByTokenHash(String tokenHash);
}
