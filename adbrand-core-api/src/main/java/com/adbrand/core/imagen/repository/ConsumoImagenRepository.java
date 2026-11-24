package com.adbrand.core.imagen.repository;

import com.adbrand.core.imagen.entity.ConsumoImagen;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumoImagenRepository extends JpaRepository<ConsumoImagen, Long> {

    // Retoques reales (no de prueba) que hizo una empresa desde una fecha: para el límite del mes.
    long countByUsuarioIdAndTipoAndSimuladoFalseAndFechaGreaterThanEqual(Long usuarioId, ConsumoImagen.Tipo tipo,
                                                                         OffsetDateTime desde);
}
