package com.adbrand.core.notificacion.recordatorio;

import com.adbrand.core.pago.entity.EstadoSuscripcion;
import com.adbrand.core.pago.repository.SuscripcionRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 52: busca las suscripciones activas que vencen en los próximos 3 días.
// El programador diario le pasa el día del negocio (zona America/Lima).
@Service
public class ConsultaVencimientosSuscripcion {

    static final int DIAS_DE_AVISO = 3;

    private final SuscripcionRepository suscripciones;

    public ConsultaVencimientosSuscripcion(SuscripcionRepository suscripciones) {
        this.suscripciones = suscripciones;
    }

    @Transactional(readOnly = true)
    public List<SuscripcionPorVencer> porVencer(LocalDate hoy) {
        return suscripciones
                .findByEstadoAndFechaVencimientoBetweenOrderByUsuarioIdAscFechaVencimientoAsc(
                        EstadoSuscripcion.ACTIVA, hoy, hoy.plusDays(DIAS_DE_AVISO))
                .stream()
                .map(suscripcion -> new SuscripcionPorVencer(
                        suscripcion,
                        ChronoUnit.DAYS.between(hoy, suscripcion.getFechaVencimiento())))
                .toList();
    }
}
