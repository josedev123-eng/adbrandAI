package com.adbrand.core.contenido.repository;

import com.adbrand.core.contenido.entity.VersionAnuncio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionAnuncioRepository extends JpaRepository<VersionAnuncio, Long> {

    List<VersionAnuncio> findByOpcionIdOrderByNumero(Long opcionId);

    Optional<VersionAnuncio> findByOpcionIdAndNumero(Long opcionId, Integer numero);
}
