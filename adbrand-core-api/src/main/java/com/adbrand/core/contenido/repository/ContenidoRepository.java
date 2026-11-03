package com.adbrand.core.contenido.repository;

import com.adbrand.core.contenido.entity.Contenido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: guardamos los anuncios. La bandeja de dudosos (HU 14) la consulta Django.
public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    // HU 37: los anuncios de una empresa, del más reciente al más antiguo (usa el índice de V35).
    List<Contenido> findByUsuarioIdOrderByFechaCreacionDescIdDesc(Long usuarioId);
}