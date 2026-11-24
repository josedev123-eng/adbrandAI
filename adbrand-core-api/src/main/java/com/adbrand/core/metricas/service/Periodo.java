package com.adbrand.core.metricas.service;

import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// HU 60: el periodo que se eligió en la app (desde y hasta, AAAA-MM-DD, los dos opcionales).
// Sin fecha, ese lado queda abierto. Usa los mismos códigos de error que el informe de la HU 40.
record Periodo(LocalDate desde, LocalDate hasta) {

    // Límites amplios para cuando falta una fecha, así la consulta no recibe nulos.
    private static final LocalDate PRIMER_DIA = LocalDate.of(2000, 1, 1);
    private static final LocalDate ULTIMO_DIA = LocalDate.of(2999, 12, 31);

    static Periodo de(String desde, String hasta) {
        LocalDate inicio = fecha(desde);
        LocalDate fin = fecha(hasta);
        // HU 60, criterio 2: con el inicio después del fin no se aplica el filtro.
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new SolicitudInvalidaException("FECHAS_INVALIDAS",
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        return new Periodo(inicio, fin);
    }

    LocalDate primerDia() {
        return desde == null ? PRIMER_DIA : desde;
    }

    LocalDate ultimoDia() {
        return hasta == null ? ULTIMO_DIA : hasta;
    }

    // Para fechas con hora (contenido.fecha_creacion): desde las 00:00 del inicio hasta antes de las 00:00 del día siguiente al fin.
    LocalDateTime inicio() {
        return primerDia().atStartOfDay();
    }

    LocalDateTime finExclusivo() {
        return ultimoDia().plusDays(1).atStartOfDay();
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
}
