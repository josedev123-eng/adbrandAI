package com.adbrand.core.video.repository;

import com.adbrand.core.video.entity.VideoIa;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoIaRepository extends JpaRepository<VideoIa, Long> {

    Optional<VideoIa> findByIdAndUsuarioId(Long id, Long usuarioId);

    // Los del mes que cuentan para el límite: los que se están creando y los que salieron (los fallidos no cuentan).
    long countByUsuarioIdAndEstadoNotAndFechaCreacionGreaterThanEqual(Long usuarioId, VideoIa.Estado estado,
                                                                       LocalDateTime desde);
}
