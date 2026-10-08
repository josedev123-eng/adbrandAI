package com.adbrand.core.brandkit.repository;

import com.adbrand.core.brandkit.entity.KitMarca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repository: guarda y consulta kits de marca.
public interface KitMarcaRepository extends JpaRepository<KitMarca, Long> {
    List<KitMarca> findByUsuarioId(Long usuarioId);
}