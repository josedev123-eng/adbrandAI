package com.adbrand.core.metricas.repository;

import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// HU 57: consultas de solo lectura sobre la tabla contenido para las métricas de la app móvil.
// No agrega métodos a ContenidoRepository: las métricas viven en su propio módulo.
// HU 60: las dos consultas cuentan solo los anuncios creados entre inicio (incluido) y fin (excluido).
public interface MetricasContenidoRepository extends Repository<Contenido, Long> {

    // Cuántos anuncios tiene la empresa en cada estado (APROBADO, DUDOSO, RECHAZADO).
    @Query("""
            SELECT c.estado AS estado, COUNT(c) AS cantidad
            FROM Contenido c
            WHERE c.usuarioId = :usuarioId
              AND c.fechaCreacion >= :inicio AND c.fechaCreacion < :fin
            GROUP BY c.estado""")
    List<ConteoPorEstado> contarPorEstado(@Param("usuarioId") Long usuarioId,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin);

    // Cuántos anuncios generó la empresa en cada mes, del más antiguo al más reciente.
    @Query("""
            SELECT EXTRACT(YEAR FROM c.fechaCreacion) AS anio, EXTRACT(MONTH FROM c.fechaCreacion) AS mes,
                   COUNT(c) AS cantidad
            FROM Contenido c
            WHERE c.usuarioId = :usuarioId
              AND c.fechaCreacion >= :inicio AND c.fechaCreacion < :fin
            GROUP BY EXTRACT(YEAR FROM c.fechaCreacion), EXTRACT(MONTH FROM c.fechaCreacion)
            ORDER BY EXTRACT(YEAR FROM c.fechaCreacion), EXTRACT(MONTH FROM c.fechaCreacion)""")
    List<ConteoPorMes> contarPorMes(@Param("usuarioId") Long usuarioId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin);

    interface ConteoPorEstado {
        EstadoContenido getEstado();

        Long getCantidad();
    }

    interface ConteoPorMes {
        Integer getAnio();

        Integer getMes();

        Long getCantidad();
    }
}
