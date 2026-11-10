package com.adbrand.core.reporte.service;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 37: arma el informe consolidado con todos los anuncios de la empresa que inició sesión.
// HU 40: se puede filtrar por periodo (desde/hasta, AAAA-MM-DD) y por campaña. Sin filtros sale igual que en la HU 37.
@Service
public class InformeService {

    private final ContenidoRepository contenidos;
    private final UsuarioRepository usuarios;
    private final PublicacionProgramadaRepository publicaciones;
    private final CampanaRepository campanasCalendario;

    @Autowired
    public InformeService(ContenidoRepository contenidos, UsuarioRepository usuarios,
                          PublicacionProgramadaRepository publicaciones, CampanaRepository campanasCalendario) {
        this.contenidos = contenidos;
        this.usuarios = usuarios;
        this.publicaciones = publicaciones;
        this.campanasCalendario = campanasCalendario;
    }

    // Lo usan las pruebas de la HU 37, que no filtran por campaña.
    public InformeService(ContenidoRepository contenidos, UsuarioRepository usuarios) {
        this(contenidos, usuarios, null, null);
    }

    @Transactional(readOnly = true)
    public InformeCampanasResponse campanas(Long usuarioId) {
        return campanas(usuarioId, null, null, null);
    }

    @Transactional(readOnly = true)
    public InformeCampanasResponse campanas(Long usuarioId, String desde, String hasta, Long campanaId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "La cuenta no existe."));

        // HU 40, criterio 2: con el inicio después del fin no se genera el informe.
        LocalDate fechaDesde = fecha(desde);
        LocalDate fechaHasta = fecha(hasta);
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new SolicitudInvalidaException("FECHAS_INVALIDAS",
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        Set<Long> deLaCampana = campanaId == null ? null : anunciosDeLaCampana(usuarioId, campanaId);

        // HU 37, criterio 1: cada anuncio con su fecha, red social, oferta y estado, del más reciente al más antiguo.
        List<CampanaInformeResponse> campanas = contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(usuarioId)
                .stream()
                .filter(contenido -> dentroDelPeriodo(contenido, fechaDesde, fechaHasta))
                .filter(contenido -> deLaCampana == null || deLaCampana.contains(contenido.getId()))
                .map(CampanaInformeResponse::desde)
                .toList();

        return new InformeCampanasResponse(
                usuario.getNombreEmpresa(),
                campanas.size(),
                contar(campanas, EstadoContenido.APROBADO),
                contar(campanas, EstadoContenido.DUDOSO),
                contar(campanas, EstadoContenido.RECHAZADO),
                campanas);
    }

    // HU 40: los anuncios que se programaron dentro de la campaña (HU 34 y 35). La de otra empresa no existe.
    private Set<Long> anunciosDeLaCampana(Long usuarioId, Long campanaId) {
        campanasCalendario.findByIdAndUsuarioId(campanaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("CAMPANA_NO_ENCONTRADA", "La campaña no existe."));
        return publicaciones.findByUsuarioIdAndCampanaId(usuarioId, campanaId).stream()
                .map(PublicacionProgramada::getContenidoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // El periodo incluye los dos días completos: desde las 00:00 del inicio hasta las 23:59 del fin.
    // Sin periodo no se mira la fecha (así funciona igual que en la HU 37).
    private static boolean dentroDelPeriodo(Contenido contenido, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return true;
        }
        LocalDate dia = contenido.getFechaCreacion().toLocalDate();
        return (desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta));
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            throw new SolicitudInvalidaException("FECHA_INVALIDA", "La fecha debe tener el formato AAAA-MM-DD.");
        }
    }

    private static long contar(List<CampanaInformeResponse> campanas, EstadoContenido estado) {
        return campanas.stream().filter(campana -> campana.estado() == estado).count();
    }
}
