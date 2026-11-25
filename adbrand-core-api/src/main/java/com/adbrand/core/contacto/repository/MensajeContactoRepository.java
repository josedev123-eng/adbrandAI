package com.adbrand.core.contacto.repository;

import com.adbrand.core.contacto.entity.MensajeContacto;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Long> {

    // Para no recibir decenas de mensajes seguidos del mismo correo.
    long countByCorreoIgnoreCaseAndFechaCreacionAfter(String correo, LocalDateTime desde);
}
