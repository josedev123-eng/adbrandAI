package com.adbrand.core.notificacion.recordatorio;

import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.notificacion.entity.TipoNotificacion;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// HU 49: cada día, a la hora del negocio, revisa las campañas que empiezan o
// terminan mañana y manda un aviso por empresa. El payload incluye el id de la
// campaña para que la app abra su detalle al tocar la notificación.
@Component
public class ProgramadorRecordatoriosCampana {

    private static final Logger log = LoggerFactory.getLogger(ProgramadorRecordatoriosCampana.class);

    private final ConsultaRecordatoriosCampana consulta;
    private final ServicioNotificaciones notificaciones;
    private final ZoneId zona;

    public ProgramadorRecordatoriosCampana(
            ConsultaRecordatoriosCampana consulta,
            ServicioNotificaciones notificaciones,
            @Value("${notificaciones.zona:America/Lima}") String zona) {
        this.consulta = consulta;
        this.notificaciones = notificaciones;
        this.zona = ZoneId.of(zona);
    }

    @Scheduled(
            cron = "${notificaciones.recordatorios.cron:0 0 9 * * *}",
            zone = "${notificaciones.zona:America/Lima}")
    public void recordarCampanasDeManana() {
        recordarCampanasDeManana(LocalDate.now(zona));
    }

    /**
     * Versión invocable desde las pruebas y desde un envío manual: recibe el día
     * actual y calcula mañana dentro del servicio de consulta.
     *
     * @return cuántos teléfonos recibieron un aviso.
     */
    public int recordarCampanasDeManana(LocalDate hoy) {
        Map<Long, List<CampanaPorRecordar>> porUsuario = consulta.agrupadasPorUsuario(hoy);
        int enviados = 0;

        for (Map.Entry<Long, List<CampanaPorRecordar>> entrada : porUsuario.entrySet()) {
            List<CampanaPorRecordar> campanas = entrada.getValue();
            if (campanas.isEmpty()) {
                continue;
            }

            Campana principal = campanas.get(0).campana();
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("type", TipoNotificacion.CAMPANA.valor());
            payload.put("campaign_id", String.valueOf(principal.getId()));
            payload.put("campaign_name", principal.getNombre());
            payload.put("campaign_count", String.valueOf(campanas.size()));

            enviados += notificaciones.enviarAUsuario(
                    entrada.getKey(),
                    titulo(campanas),
                    cuerpo(campanas),
                    payload);
        }

        if (enviados > 0) {
            log.info("Recordatorios de campañas: {} aviso(s) para {} empresa(s).",
                    enviados, porUsuario.size());
        }
        return enviados;
    }

    private String titulo(List<CampanaPorRecordar> campanas) {
        return campanas.size() == 1
                ? "Mañana es clave para tu campaña"
                : "Mañana empiezan o terminan " + campanas.size() + " campañas";
    }

    private String cuerpo(List<CampanaPorRecordar> campanas) {
        if (campanas.size() == 1) {
            return campanas.get(0).mensaje() + " Ábrela para revisar los detalles.";
        }
        return String.join(" ", campanas.stream().map(CampanaPorRecordar::mensaje).toList())
                + " Toca para revisar tu calendario.";
    }
}
