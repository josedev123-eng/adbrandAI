package com.adbrand.core.contenido.repository;

import com.adbrand.core.contenido.entity.Contenido;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: por ahora solo guardamos. La bandeja de dudosos (HU 14) la consulta Django.
public interface ContenidoRepository extends JpaRepository<Contenido, Long> {
}