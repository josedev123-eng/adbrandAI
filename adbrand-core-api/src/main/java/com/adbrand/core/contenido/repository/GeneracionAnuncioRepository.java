package com.adbrand.core.contenido.repository;

import com.adbrand.core.contenido.entity.GeneracionAnuncio;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneracionAnuncioRepository extends JpaRepository<GeneracionAnuncio, Long> {

    // Cuántas veces pidió anuncios (4 opciones cada vez) desde una fecha: "Tu uso este mes" en Planes.
    long countByUsuarioIdAndFechaCreacionGreaterThanEqual(Long usuarioId, LocalDateTime desde);
}
