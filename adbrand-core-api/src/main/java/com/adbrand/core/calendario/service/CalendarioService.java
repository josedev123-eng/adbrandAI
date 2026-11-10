package com.adbrand.core.calendario.service;

import com.adbrand.core.calendario.dto.CrearPublicacionRequest;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 33: calendario mensual de publicaciones. El mes llega como AAAA-MM; sin mes se usa el actual.
// HU 35: cada publicación puede ser de una campaña, y el calendario se puede filtrar por campaña.
// HU 34: al programar se revisa que la fecha no haya pasado y que el anuncio esté aprobado.
@Service
public class CalendarioService {

    private final PublicacionProgramadaRepository repositorio;
    private final CampanaRepository campanas;
    private final ContenidoRepository contenidos;

    @Autowired
    public CalendarioService(PublicacionProgramadaRepository repositorio, CampanaRepository campanas,
                             ContenidoRepository contenidos) {
        this.repositorio = repositorio;
        this.campanas = campanas;
        this.contenidos = contenidos;
    }

    // Lo usan las pruebas de la HU 33, que no tienen campañas ni anuncios.
    public CalendarioService(PublicacionProgramadaRepository repositorio) {
        this(repositorio, null, null);
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
        return conDetalle(usuarioId, publicaciones);
    }

    @Transactional
    public PublicacionResponse crear(Long usuarioId, CrearPublicacionRequest datos) {
        // HU 34, criterio 2: no se programa en una fecha u hora que ya pasó.
        if (LocalDateTime.of(datos.fecha(), datos.hora()).isBefore(LocalDateTime.now())) {
            throw new SolicitudInvalidaException("FECHA_PASADA",
                    "No puedes programar en una fecha u hora que ya pasó.");
        }

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

        // HU 34: si eligió un anuncio, tiene que ser de su empresa y estar aprobado.
        Contenido contenido = null;
        if (datos.contenidoId() != null) {
            contenido = contenidoAprobado(usuarioId, datos.contenidoId());
            publicacion.setContenidoId(contenido.getId());
        }

        repositorio.saveAndFlush(publicacion);
        return PublicacionResponse.desde(publicacion, campana, contenido);
    }

    // HU 36: mueve la publicación a otro día (al arrastrarla en el calendario). La hora se mantiene.
    @Transactional
    public PublicacionResponse reprogramar(Long usuarioId, Long publicacionId, LocalDate fecha) {
        PublicacionProgramada publicacion = repositorio.findByIdAndUsuarioId(publicacionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("PUBLICACION_NO_ENCONTRADA",
                        "La publicación no existe."));

        // Una publicación ya publicada o cancelada se queda en su día.
        if (publicacion.getEstado() != EstadoPublicacion.PROGRAMADA) {
            throw new SolicitudInvalidaException("PUBLICACION_NO_MOVIBLE",
                    "Solo se pueden mover las publicaciones programadas.");
        }

        // HU 36, criterio 2: no se mueve a un día (con su hora) que ya pasó.
        if (LocalDateTime.of(fecha, publicacion.getHora()).isBefore(LocalDateTime.now())) {
            throw new SolicitudInvalidaException("FECHA_PASADA",
                    "No puedes mover una publicación a un día que ya pasó.");
        }

        publicacion.setFecha(fecha);
        repositorio.saveAndFlush(publicacion);
        return conDetalle(usuarioId, List.of(publicacion)).get(0);
    }

    // Agrega la campaña (nombre y color) y el anuncio (oferta) de cada publicación.
    // Solo consulta lo que hace falta: si ninguna publicación tiene campaña o anuncio, no se consulta.
    private List<PublicacionResponse> conDetalle(Long usuarioId, List<PublicacionProgramada> publicaciones) {
        boolean algunaConCampana = publicaciones.stream().anyMatch(p -> p.getCampanaId() != null);
        Map<Long, Campana> campanaPorId = algunaConCampana
                ? campanas.findByUsuarioIdOrderByFechaInicioAscIdAsc(usuarioId).stream()
                        .collect(Collectors.toMap(Campana::getId, Function.identity()))
                : Map.of();

        List<Long> idsContenido = publicaciones.stream()
                .map(PublicacionProgramada::getContenidoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Contenido> contenidoPorId = idsContenido.isEmpty()
                ? Map.of()
                : contenidos.findAllById(idsContenido).stream()
                        .collect(Collectors.toMap(Contenido::getId, Function.identity()));

        return publicaciones.stream()
                .map(p -> PublicacionResponse.desde(p,
                        p.getCampanaId() == null ? null : campanaPorId.get(p.getCampanaId()),
                        p.getContenidoId() == null ? null : contenidoPorId.get(p.getContenidoId())))
                .toList();
    }

    // La campaña de otra empresa se trata igual que una que no existe.
    private Campana campanaDeLaEmpresa(Long usuarioId, Long campanaId) {
        return campanas.findByIdAndUsuarioId(campanaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("CAMPANA_NO_ENCONTRADA",
                        "La campaña no existe."));
    }

    // HU 34, criterio 1 y 2: solo se programa un anuncio aprobado. El de otra empresa se trata como inexistente.
    private Contenido contenidoAprobado(Long usuarioId, Long contenidoId) {
        Contenido contenido = contenidos.findById(contenidoId)
                .filter(encontrado -> encontrado.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("CONTENIDO_NO_ENCONTRADO",
                        "El anuncio no existe."));

        if (contenido.getEstado() == EstadoContenido.RECHAZADO) {
            throw new SolicitudInvalidaException("CONTENIDO_RECHAZADO",
                    "Este anuncio fue rechazado y no se puede programar.");
        }
        if (contenido.getEstado() != EstadoContenido.APROBADO) {
            throw new SolicitudInvalidaException("CONTENIDO_NO_APROBADO",
                    "Este anuncio todavía está en revisión. Solo se programan anuncios aprobados.");
        }
        return contenido;
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
