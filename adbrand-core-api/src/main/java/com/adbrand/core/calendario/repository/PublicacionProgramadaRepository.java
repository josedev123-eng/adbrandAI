package com.adbrand.core.calendario.repository;

import com.adbrand.core.calendario.entity.PublicacionProgramada;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionProgramadaRepository extends JpaRepository<PublicacionProgramada, Long> {

    // HU 33: publicaciones del mes pedido, ordenadas como se ven en el calendario.
    List<PublicacionProgramada> findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(
            Long usuarioId, LocalDate desde, LocalDate hasta);

    // HU 35: lo mismo, pero solo las publicaciones de una campaña.
    List<PublicacionProgramada> findByUsuarioIdAndCampanaIdAndFechaBetweenOrderByFechaAscHoraAsc(
            Long usuarioId, Long campanaId, LocalDate desde, LocalDate hasta);

    // HU 36: una publicación solo se mueve si es de la empresa que inició sesión.
    Optional<PublicacionProgramada> findByIdAndUsuarioId(Long id, Long usuarioId);

    // HU 40: publicaciones de una campaña, para filtrar el informe por los anuncios que se programaron en ella.
    List<PublicacionProgramada> findByUsuarioIdAndCampanaId(Long usuarioId, Long campanaId);
}
