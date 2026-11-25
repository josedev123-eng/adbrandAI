package com.adbrand.core.notificacion.recordatorio;

import com.adbrand.core.notificacion.entity.TipoNotificacion;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// HU 52: cada día avisa a las empresas cuyo plan vence en los próximos 3 días.
// El payload lleva el plan, si la renovación es automática y cuántos días quedan,
// para que la app abra la pantalla del plan con el contexto correcto.
@Component
public class ProgramadorRecordatoriosSuscripcion {

    private static final Logger log =
            LoggerFactory.getLogger(ProgramadorRecordatoriosSuscripcion.class);

    private final ConsultaVencimientosSuscripcion consulta;
    private final ServicioNotificaciones notificaciones;
    private final ZoneId zona;

    public ProgramadorRecordatoriosSuscripcion(
            ConsultaVencimientosSuscripcion consulta,
            ServicioNotificaciones notificaciones,
            @Value("${notificaciones.zona:America/Lima}") String zona) {
        this.consulta = consulta;
        this.notificaciones = notificaciones;
        this.zona = ZoneId.of(zona);
    }

    @Scheduled(
            cron = "${notificaciones.suscripciones.cron:0 30 9 * * *}",
            zone = "${notificaciones.zona:America/Lima}")
    public void recordarVencimientos() {
        recordarVencimientos(LocalDate.now(zona));
    }

    /**
     * Versión invocable desde las pruebas y desde un envío manual.
     *
     * @return cuántos teléfonos recibieron el aviso.
     */
    public int recordarVencimientos(LocalDate hoy) {
        int enviados = 0;

        for (SuscripcionPorVencer aviso : consulta.porVencer(hoy)) {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("type", TipoNotificacion.SUSCRIPCION.valor());
            payload.put("plan", aviso.suscripcion().getPlan());
            payload.put("renewal", aviso.modo());
            payload.put("days_left", String.valueOf(aviso.diasRestantes()));
            payload.put("expires_at", aviso.suscripcion().getFechaVencimiento().toString());

            enviados += notificaciones.enviarAUsuario(
                    aviso.suscripcion().getUsuarioId(),
                    aviso.titulo(),
                    aviso.mensaje(),
                    payload);
        }

        if (enviados > 0) {
            log.info("Recordatorios de suscripción: {} aviso(s) enviados.", enviados);
        }
        return enviados;
    }
}
