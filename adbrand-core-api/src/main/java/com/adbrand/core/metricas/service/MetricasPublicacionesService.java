package com.adbrand.core.metricas.service;

import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.metricas.dto.MetricasPublicacionesResponse;
import com.adbrand.core.metricas.dto.MetricasPublicacionesResponse.ProximaPublicacion;
import com.adbrand.core.metricas.dto.MetricasPublicacionesResponse.PublicacionesPorRed;
import com.adbrand.core.metricas.repository.MetricasPublicacionRepository;
import com.adbrand.core.metricas.repository.MetricasPublicacionRepository.ConteoPorRedYEstado;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 58: arma las métricas de las publicaciones del calendario de la empresa que inició sesión.
// Cuenta las mismas filas que muestra el calendario de la web, así los números coinciden (criterio 2).
@Service
public class MetricasPublicacionesService {

    // Cuántas próximas publicaciones se muestran en la app.
    static final int PROXIMAS = 5;

    private final MetricasPublicacionRepository repositorio;
    private final CampanaRepository campanas;

    public MetricasPublicacionesService(MetricasPublicacionRepository repositorio, CampanaRepository campanas) {
        this.repositorio = repositorio;
        this.campanas = campanas;
    }

    @Transactional(readOnly = true)
    public MetricasPublicacionesResponse publicaciones(Long usuarioId) {
        // La consulta solo trae las combinaciones que tienen publicaciones; las demás quedan en cero.
        Map<RedSocialPublicacion, Map<EstadoPublicacion, Long>> conteos = new EnumMap<>(RedSocialPublicacion.class);
        for (ConteoPorRedYEstado conteo : repositorio.contarPorRedYEstado(usuarioId)) {
            conteos.computeIfAbsent(conteo.getRedSocial(), red -> new EnumMap<>(EstadoPublicacion.class))
                    .put(conteo.getEstado(), conteo.getCantidad());
        }

        List<PublicacionesPorRed> porRed = Arrays.stream(RedSocialPublicacion.values())
                .map(red -> new PublicacionesPorRed(
                        red.name(),
                        contar(conteos, red, EstadoPublicacion.PROGRAMADA),
                        contar(conteos, red, EstadoPublicacion.PUBLICADA)))
                .toList();

        return new MetricasPublicacionesResponse(
                total(conteos, EstadoPublicacion.PROGRAMADA),
                total(conteos, EstadoPublicacion.PUBLICADA),
                total(conteos, EstadoPublicacion.CANCELADA),
                porRed,
                proximas(usuarioId));
    }

    private List<ProximaPublicacion> proximas(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        List<PublicacionProgramada> siguientes = repositorio.proximas(
                usuarioId, ahora.toLocalDate(), ahora.toLocalTime(), PageRequest.of(0, PROXIMAS));
        if (siguientes.isEmpty()) {
            return List.of();
        }

        // Nombre y color de la campaña de cada una (HU 35), como en el calendario.
        Map<Long, Campana> campanaPorId = campanas.findByUsuarioIdOrderByFechaInicioAscIdAsc(usuarioId).stream()
                .collect(Collectors.toMap(Campana::getId, Function.identity()));

        return siguientes.stream()
                .map(p -> {
                    Campana campana = p.getCampanaId() == null ? null : campanaPorId.get(p.getCampanaId());
                    return new ProximaPublicacion(p.getId(), p.getTitulo(), p.getRedSocial().name(), p.getFecha(), p.getHora(),
                            campana == null ? null : campana.getNombre(),
                            campana == null ? null : campana.getColor());
                })
                .toList();
    }

    private static long contar(Map<RedSocialPublicacion, Map<EstadoPublicacion, Long>> conteos,
                               RedSocialPublicacion red, EstadoPublicacion estado) {
        return conteos.getOrDefault(red, Map.of()).getOrDefault(estado, 0L);
    }

    private static long total(Map<RedSocialPublicacion, Map<EstadoPublicacion, Long>> conteos, EstadoPublicacion estado) {
        return Arrays.stream(RedSocialPublicacion.values()).mapToLong(red -> contar(conteos, red, estado)).sum();
    }
}
