package com.adbrand.core.calendario.service;

import com.adbrand.core.calendario.dto.CampanaResponse;
import com.adbrand.core.calendario.dto.CrearCampanaRequest;
import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 35: campañas y eventos especiales de la empresa que inició sesión.
@Service
public class CampanaService {

    private final CampanaRepository repositorio;

    public CampanaService(CampanaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<CampanaResponse> listar(Long usuarioId) {
        return repositorio.findByUsuarioIdOrderByFechaInicioAscIdAsc(usuarioId)
                .stream()
                .map(CampanaResponse::desde)
                .toList();
    }

    @Transactional
    public CampanaResponse crear(Long usuarioId, CrearCampanaRequest datos) {
        if (datos.fechaFin().isBefore(datos.fechaInicio())) {
            throw new SolicitudInvalidaException("FECHAS_INVALIDAS",
                    "La fecha de fin no puede ser anterior a la fecha de inicio.");
        }

        Campana campana = new Campana();
        campana.setUsuarioId(usuarioId);
        campana.setNombre(datos.nombre());
        // Siempre en mayúsculas, como los colores del kit de marca.
        campana.setColor(datos.color().toUpperCase(Locale.ROOT));
        campana.setTipo(datos.tipo());
        campana.setFechaInicio(datos.fechaInicio());
        campana.setFechaFin(datos.fechaFin());
        repositorio.saveAndFlush(campana);
        return CampanaResponse.desde(campana);
    }
}
