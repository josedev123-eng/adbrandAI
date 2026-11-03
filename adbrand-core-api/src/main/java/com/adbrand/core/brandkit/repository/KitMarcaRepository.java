package com.adbrand.core.brandkit.repository;

import com.adbrand.core.brandkit.entity.KitMarca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repository: guarda y consulta kits de marca.
public interface KitMarcaRepository extends JpaRepository<KitMarca, Long> {
    List<KitMarca> findByUsuarioId(Long usuarioId);

    // HU 31: el kit editado más reciente es la identidad que usan los anuncios nuevos.
    Optional<KitMarca> findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(Long usuarioId);
}