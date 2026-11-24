package com.adbrand.core.ia.repository;

import com.adbrand.core.ia.entity.LimiteTokens;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimiteTokensRepository extends JpaRepository<LimiteTokens, Long> {

    Optional<LimiteTokens> findByUsuarioId(Long usuarioId);

    // El límite por defecto (plan gratuito) es la fila sin empresa.
    Optional<LimiteTokens> findFirstByUsuarioIdIsNull();
}
