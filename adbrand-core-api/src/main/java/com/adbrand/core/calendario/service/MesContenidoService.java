package com.adbrand.core.calendario.service;

import com.adbrand.core.calendario.dto.MesContenidoResponse;
import com.adbrand.core.calendario.dto.MesContenidoResponse.FechaSugerida;
import com.adbrand.core.calendario.dto.MesContenidoResponse.PublicacionSugerida;
import com.adbrand.core.calendario.service.FechasPeruanas.FechaComercial;
import com.adbrand.core.negocio.repository.PerfilNegocioRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 63: "Tu mes de contenido". Arma, sin IA (no gasta tokens), las fechas peruanas del mes con ideas según
// el rubro del perfil, y un plan de 3 publicaciones por semana (martes, jueves y sábado) que mezcla oferta,
// producto estrella y "detrás de cámaras". El día de una fecha especial, la publicación es sobre esa fecha.
@Service
public class MesContenidoService {

    private static final Set<DayOfWeek> DIAS_DE_PUBLICAR = Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
    // Si el mes ya casi termina, también se muestran las fechas de los próximos días del mes siguiente.
    private static final int DIAS_ADELANTE = 10;

    private final PerfilNegocioRepository perfiles;
    private final Clock reloj;

    @Autowired
    public MesContenidoService(PerfilNegocioRepository perfiles) {
        this(perfiles, Clock.system(java.time.ZoneId.of("America/Lima")));
    }

    MesContenidoService(PerfilNegocioRepository perfiles, Clock reloj) {
        this.perfiles = perfiles;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public MesContenidoResponse generar(Long usuarioId, Integer anio, Integer mes) {
        LocalDate hoy = LocalDate.now(reloj);
        YearMonth periodo = anio != null && mes != null ? YearMonth.of(anio, mes) : YearMonth.from(hoy);
        String rubro = perfiles.findByUsuarioId(usuarioId).map(p -> p.getRubro()).orElse(null);
        return armar(periodo, hoy, rubro);
    }

    static MesContenidoResponse armar(YearMonth periodo, LocalDate hoy, String rubro) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fin = periodo.atEndOfMonth();
        LocalDate finConMargen = fin.plusDays(DIAS_ADELANTE);

        List<FechaComercial> todas = new ArrayList<>(FechasPeruanas.delAnio(periodo.getYear()));
        if (finConMargen.getYear() != periodo.getYear()) {
            todas.addAll(FechasPeruanas.delAnio(finConMargen.getYear()));
        }

        List<FechaSugerida> fechas = todas.stream()
                .filter(f -> !f.fecha().isBefore(inicio) && !f.fecha().isAfter(finConMargen))
                .map(f -> new FechaSugerida(f.fecha(), f.nombre(), f.ideaPara(rubro), ChronoUnit.DAYS.between(hoy, f.fecha())))
                .toList();

        List<PublicacionSugerida> plan = new ArrayList<>();
        int turno = 0;
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            LocalDate actual = dia;
            FechaComercial especial = todas.stream().filter(f -> f.fecha().equals(actual)).findFirst().orElse(null);
            if (especial != null) {
                plan.add(new PublicacionSugerida(dia, "FECHA_ESPECIAL", especial.nombre(), especial.ideaPara(rubro)));
            } else if (DIAS_DE_PUBLICAR.contains(dia.getDayOfWeek())) {
                plan.add(sugerencia(dia, turno++ % 3));
            }
        }
        return new MesContenidoResponse(periodo.getYear(), periodo.getMonthValue(), rubro, fechas, plan);
    }

    // La mezcla que recomiendan las guías de redes: vender, mostrar y generar confianza, por turnos.
    private static PublicacionSugerida sugerencia(LocalDate dia, int turno) {
        return switch (turno) {
            case 0 -> new PublicacionSugerida(dia, "OFERTA", "Oferta de la semana",
                    "Una promoción concreta con precio y hasta cuándo vale (ej.: 2x1 hasta el sábado).");
            case 1 -> new PublicacionSugerida(dia, "PRODUCTO_ESTRELLA", "Tu producto estrella",
                    "Muestra lo que más te piden y por qué les encanta.");
            default -> new PublicacionSugerida(dia, "DETRAS_DE_CAMARAS", "Detrás de cámaras",
                    "Cómo preparas tu producto o quién está detrás del negocio: genera confianza.");
        };
    }
}
