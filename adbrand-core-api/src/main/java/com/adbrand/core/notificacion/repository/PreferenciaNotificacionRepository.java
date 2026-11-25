package com.adbrand.core.notificacion.repository;

import com.adbrand.core.notificacion.entity.PreferenciaNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

// HU 51: preferencias de avisos de cada empresa.
public interface PreferenciaNotificacionRepository
        extends JpaRepository<PreferenciaNotificacion, Long> {
}
