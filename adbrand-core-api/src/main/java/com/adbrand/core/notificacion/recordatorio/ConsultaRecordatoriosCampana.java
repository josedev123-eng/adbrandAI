package com.adbrand.core.notificacion.recordatorio;

import com.adbrand.core.calendario.repository.CampanaRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 49: consulta de campañas con una fecha clave. La zona horaria la decide el
// programador diario (HU 49, commit siguiente) para que "mañana" sea el día del
// negocio y no el del servidor.
@Service
public class ConsultaRecordatoriosCampana {

    private final CampanaRepository campanas;

    public ConsultaRecordatoriosCampana(CampanaRepository campanas) {
        this.campanas = campanas;
    }

    @Transactional(readOnly = true)
    public List<CampanaPorRecordar> paraFecha(LocalDate fecha) {
        return campanas.buscarConFechaClave(fecha).stream()
                .map(campana -> new CampanaPorRecordar(
                        campana,
                        fecha.equals(campana.getFechaInicio()),
                        fecha.equals(campana.getFechaFin())))
                .toList();
    }

    // Como el aviso se envía hoy, la fecha clave es la de mañana.
    public List<CampanaPorRecordar> deManana(LocalDate hoy) {
        return paraFecha(hoy.plusDays(1));
    }

    // Agrupa por empresa: un solo aviso por usuario aunque tenga varias campañas.
    public Map<Long, List<CampanaPorRecordar>> agrupadasPorUsuario(LocalDate hoy) {
        return deManana(hoy).stream()
                .collect(Collectors.groupingBy(
                        recordatorio -> recordatorio.campana().getUsuarioId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }
}
