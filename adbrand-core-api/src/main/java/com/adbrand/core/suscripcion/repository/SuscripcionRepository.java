package com.adbrand.core.suscripcion.repository;

import com.adbrand.core.suscripcion.entity.Suscripcion;
import com.adbrand.core.suscripcion.entity.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// Repository: consultas de suscripciones con deuda o vencidas (HU 8)
@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    @Query("SELECT s FROM Suscripcion s WHERE s.estado = :estado")
    List<Suscripcion> findByEstado(@Param("estado") EstadoSuscripcion estado);

    @Query("SELECT s FROM Suscripcion s WHERE s.fechaVencimiento < :hoy AND s.estado = 'ACTIVA'")
    List<Suscripcion> findVencidas(@Param("hoy") LocalDate hoy);

    @Query("SELECT s FROM Suscripcion s WHERE s.estado = 'PENDIENTE_PAGO'")
    List<Suscripcion> findPendientesPago();
}