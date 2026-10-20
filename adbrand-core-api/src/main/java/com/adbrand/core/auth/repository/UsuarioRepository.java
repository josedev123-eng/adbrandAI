package com.adbrand.core.auth.repository;

import com.adbrand.core.auth.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: el correo se guarda en minúsculas, así que se busca tal cual.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreo(String correo);

    // HU 23: el RUC ya lo tiene otra cuenta.
    boolean existsByRucAndIdNot(String ruc, Long id);
}
