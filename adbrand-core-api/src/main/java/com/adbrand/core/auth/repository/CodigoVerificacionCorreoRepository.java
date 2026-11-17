package com.adbrand.core.auth.repository;

import com.adbrand.core.auth.entity.CodigoVerificacionCorreo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: los códigos para verificar el correo que un usuario todavía no usó, el más nuevo primero.
public interface CodigoVerificacionCorreoRepository extends JpaRepository<CodigoVerificacionCorreo, Long> {

    List<CodigoVerificacionCorreo> findByUsuarioIdAndUsadoFalseOrderByFechaCreacionDesc(Long usuarioId);
}
