package com.adbrand.core.calendario.repository;

import com.adbrand.core.calendario.entity.PublicacionProgramada;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacionProgramadaRepository extends JpaRepository<PublicacionProgramada, Long> {

    // HU 33: publicaciones del mes pedido, ordenadas como se ven en el calendario.
    List<PublicacionProgramada> findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(
            Long usuarioId, LocalDate desde, LocalDate hasta);
}
