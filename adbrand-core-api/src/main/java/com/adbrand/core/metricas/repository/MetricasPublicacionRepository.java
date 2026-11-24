package com.adbrand.core.metricas.repository;

import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// HU 58: consultas de solo lectura sobre publicacion_programada (el calendario de la HU 33 a 36).
public interface MetricasPublicacionRepository extends Repository<PublicacionProgramada, Long> {

    // Cuántas publicaciones tiene la empresa por red social y por estado.
    // HU 60: solo las de un día entre desde y hasta (los dos incluidos).
    @Query("""
            SELECT p.redSocial AS redSocial, p.estado AS estado, COUNT(p) AS cantidad
            FROM PublicacionProgramada p
            WHERE p.usuarioId = :usuarioId
              AND p.fecha BETWEEN :desde AND :hasta
            GROUP BY p.redSocial, p.estado""")
    List<ConteoPorRedYEstado> contarPorRedYEstado(@Param("usuarioId") Long usuarioId,
                                                  @Param("desde") LocalDate desde,
                                                  @Param("hasta") LocalDate hasta);

    // Las siguientes publicaciones programadas desde este momento, en el orden en que salen.
    @Query("""
            SELECT p FROM PublicacionProgramada p
            WHERE p.usuarioId = :usuarioId
              AND p.estado = com.adbrand.core.calendario.entity.EstadoPublicacion.PROGRAMADA
              AND (p.fecha > :hoy OR (p.fecha = :hoy AND p.hora >= :ahora))
            ORDER BY p.fecha, p.hora, p.id""")
    List<PublicacionProgramada> proximas(@Param("usuarioId") Long usuarioId, @Param("hoy") LocalDate hoy,
                                         @Param("ahora") LocalTime ahora, Pageable limite);

    interface ConteoPorRedYEstado {
        RedSocialPublicacion getRedSocial();

        EstadoPublicacion getEstado();

        Long getCantidad();
    }
}
