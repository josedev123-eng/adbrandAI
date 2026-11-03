package com.adbrand.core.calendario.service;

import com.adbrand.core.calendario.dto.CrearPublicacionRequest;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 33: calendario mensual de publicaciones. El mes llega como AAAA-MM; sin mes se usa el actual.
// HU 35: cada publicación puede ser de una campaña, y el calendario se puede filtrar por campaña.
@Service
public class CalendarioService {

    private final PublicacionProgramadaRepository repositorio;
    private final CampanaRepository campanas;

    @Autowired
    public CalendarioService(PublicacionProgramadaRepository repositorio, CampanaRepository campanas) {
        this.repositorio = repositorio;
        this.campanas = campanas;
    }

    // Lo usan las pruebas de la HU 33, que no tienen campañas.
    public CalendarioService(PublicacionProgramadaRepository repositorio) {
        this(repositorio, null);
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponse> listar(Long usuarioId, String mes) {
        return listar(usuarioId, mes, null);
    }

    // HU 35: con campanaId solo salen las publicaciones de esa campaña.
    @Transactional(readOnly = true)
    public List<PublicacionResponse> listar(Long usuarioId, String mes, Long campanaId) {
        YearMonth periodo = periodo(mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        List<PublicacionProgramada> publicaciones;
        if (campanaId == null) {
            publicaciones = repositorio.findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(usuarioId, desde, hasta);
        } else {
            campanaDeLaEmpresa(usuarioId, campanaId);
            publicaciones = repositorio.findByUsuarioIdAndCampanaIdAndFechaBetweenOrderByFechaAscHoraAsc(
                    usuarioId, campanaId, desde, hasta);
        }
        return conCampana(usuarioId, publicaciones);
    }

    @Transactional
    public PublicacionResponse crear(Long usuarioId, CrearPublicacionRequest datos) {
        PublicacionProgramada publicacion = new PublicacionProgramada();
        publicacion.setUsuarioId(usuarioId);
        publicacion.setTitulo(datos.titulo().trim());
        publicacion.setRedSocial(datos.redSocial());
        publicacion.setFecha(datos.fecha());
        publicacion.setHora(datos.hora());
        publicacion.setEstado(EstadoPublicacion.PROGRAMADA);

        // HU 35: si eligió una campaña, tiene que ser de su empresa.
        Campana campana = null;
        if (datos.campanaId() != null) {
            campana = campanaDeLaEmpresa(usuarioId, datos.campanaId());
            publicacion.setCampanaId(campana.getId());
        }
        repositorio.saveAndFlush(publicacion);
        return PublicacionResponse.desde(publicacion, campana);
    }

    // Agrega el nombre y el color de la campaña a cada publicación, para pintarla en el calendario.
    // Solo consulta las campañas si alguna publicación tiene una.
    private List<PublicacionResponse> conCampana(Long usuarioId, List<PublicacionProgramada> publicaciones) {
        boolean algunaConCampana = publicaciones.stream().anyMatch(p -> p.getCampanaId() != null);
        Map<Long, Campana> porId = algunaConCampana
                ? campanas.findByUsuarioIdOrderByFechaInicioAscIdAsc(usuarioId).stream()
                        .collect(Collectors.toMap(Campana::getId, Function.identity()))
                : Map.of();

        return publicaciones.stream()
                .map(p -> PublicacionResponse.desde(p, p.getCampanaId() == null ? null : porId.get(p.getCampanaId())))
                .toList();
    }

    // La campaña de otra empresa se trata igual que una que no existe.
    private Campana campanaDeLaEmpresa(Long usuarioId, Long campanaId) {
        return campanas.findByIdAndUsuarioId(campanaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("CAMPANA_NO_ENCONTRADA",
                        "La campaña no existe."));
    }

    private YearMonth periodo(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes.trim());
        } catch (DateTimeParseException ex) {
            throw new SolicitudInvalidaException("MES_INVALIDO", "El mes debe tener el formato AAAA-MM.");
        }
    }
}
