package com.adbrand.core.calendario.dto;

import java.time.LocalDate;
import java.util.List;

// HU 63: "Tu mes de contenido". Las fechas peruanas del mes (y las de los próximos días si el mes está por
// terminar) con una idea de anuncio según el rubro, y un plan de 3 publicaciones por semana.
public record MesContenidoResponse(
        int anio,
        int mes,
        String rubro,
        List<FechaSugerida> fechas,
        List<PublicacionSugerida> plan) {

    // diasFaltan: cuántos días faltan desde hoy (negativo si ya pasó).
    public record FechaSugerida(LocalDate fecha, String nombre, String idea, long diasFaltan) {
    }

    // tipo: OFERTA, PRODUCTO_ESTRELLA, DETRAS_DE_CAMARAS o FECHA_ESPECIAL; idea: qué publicar ese día.
    public record PublicacionSugerida(LocalDate fecha, String tipo, String titulo, String idea) {
    }
}
