package com.adbrand.core.calendario.service;

import com.adbrand.core.calendario.dto.CrearPublicacionRequest;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 33: calendario mensual de publicaciones. El mes llega como AAAA-MM; sin mes se usa el actual.
@Service
public class CalendarioService {

    private final PublicacionProgramadaRepository repositorio;

    public CalendarioService(PublicacionProgramadaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponse> listar(Long usuarioId, String mes) {
        YearMonth periodo = periodo(mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        return repositorio.findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(usuarioId, desde, hasta)
                .stream()
                .map(PublicacionResponse::desde)
                .toList();
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
        repositorio.saveAndFlush(publicacion);
        return PublicacionResponse.desde(publicacion);
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
