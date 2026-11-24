package com.adbrand.core.previsualizacion.repository;

import com.adbrand.core.previsualizacion.entity.AnuncioCompartido;
import org.springframework.data.jpa.repository.JpaRepository;

// HU 47: registro de anuncios compartidos.
public interface AnuncioCompartidoRepository extends JpaRepository<AnuncioCompartido, Long> {

    long countByContenidoId(Long contenidoId);
}
