package com.adbrand.core.metricas.service;

import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.metricas.dto.MetricasContenidoResponse;
import com.adbrand.core.metricas.dto.MetricasContenidoResponse.ContenidoPorMes;
import com.adbrand.core.metricas.repository.MetricasContenidoRepository;
import com.adbrand.core.metricas.repository.MetricasContenidoRepository.ConteoPorEstado;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 57: arma las métricas del contenido de la empresa que inició sesión.
// HU 60: se puede filtrar por periodo (desde/hasta, AAAA-MM-DD). Sin fechas cuenta todo, como en la HU 57.
@Service
public class MetricasContenidoService {

    private final MetricasContenidoRepository repositorio;

    public MetricasContenidoService(MetricasContenidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public MetricasContenidoResponse contenido(Long usuarioId) {
        return contenido(usuarioId, null, null);
    }

    @Transactional(readOnly = true)
    public MetricasContenidoResponse contenido(Long usuarioId, String desde, String hasta) {
        Periodo periodo = Periodo.de(desde, hasta);

        // La consulta solo trae los estados que tienen anuncios; los demás quedan en cero.
        Map<EstadoContenido, Long> porEstado = new EnumMap<>(EstadoContenido.class);
        for (ConteoPorEstado conteo : repositorio.contarPorEstado(usuarioId, periodo.inicio(), periodo.finExclusivo())) {
            porEstado.put(conteo.getEstado(), conteo.getCantidad());
        }
        long aprobados = porEstado.getOrDefault(EstadoContenido.APROBADO, 0L);
        long enRevision = porEstado.getOrDefault(EstadoContenido.DUDOSO, 0L);
        long rechazados = porEstado.getOrDefault(EstadoContenido.RECHAZADO, 0L);

        List<ContenidoPorMes> porMes = repositorio.contarPorMes(usuarioId, periodo.inicio(), periodo.finExclusivo()).stream()
                .map(conteo -> new ContenidoPorMes(
                        "%04d-%02d".formatted(conteo.getAnio(), conteo.getMes()), conteo.getCantidad()))
                .toList();

        return new MetricasContenidoResponse(aprobados + enRevision + rechazados, aprobados, enRevision, rechazados, porMes);
    }
}
