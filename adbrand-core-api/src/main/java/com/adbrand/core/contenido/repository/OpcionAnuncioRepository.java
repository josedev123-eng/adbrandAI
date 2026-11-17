package com.adbrand.core.contenido.repository;

import com.adbrand.core.contenido.entity.OpcionAnuncio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpcionAnuncioRepository extends JpaRepository<OpcionAnuncio, Long> {

    // La opción solo se encuentra si su generación es de la empresa del token.
    Optional<OpcionAnuncio> findByIdAndGeneracionUsuarioId(Long id, Long usuarioId);

    List<OpcionAnuncio> findByGeneracionIdOrderByNumero(Long generacionId);

    // HU 46: la opción elegida de un anuncio guardado (de ahí sale su imagen).
    Optional<OpcionAnuncio> findFirstByContenidoIdAndElegidaTrue(Long contenidoId);
}
