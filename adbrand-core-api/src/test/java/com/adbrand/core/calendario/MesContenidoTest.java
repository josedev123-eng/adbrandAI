package com.adbrand.core.calendario;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.calendario.dto.MesContenidoResponse;
import com.adbrand.core.calendario.dto.MesContenidoResponse.FechaSugerida;
import com.adbrand.core.calendario.service.FechasPeruanas;
import com.adbrand.core.calendario.service.FechasPeruanas.FechaComercial;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

// HU 63: fechas peruanas que cambian cada año, ideas por rubro y el plan de 3 publicaciones por semana.
class MesContenidoTest {

    private static LocalDate fechaDe(int anio, String nombre) {
        return FechasPeruanas.delAnio(anio).stream().filter(f -> f.nombre().equals(nombre))
                .map(FechaComercial::fecha).findFirst().orElseThrow();
    }

    @Test
    void lasFechasQueCambianSeCalculanBienEn2026() {
        assertThat(fechaDe(2026, "Día de la Madre")).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(fechaDe(2026, "Día del Padre")).isEqualTo(LocalDate.of(2026, 6, 21));
        assertThat(fechaDe(2026, "Día del Pollo a la Brasa")).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(fechaDe(2026, "Día del Pisco Sour")).isEqualTo(LocalDate.of(2026, 2, 7));
        assertThat(fechaDe(2026, "Black Friday")).isEqualTo(LocalDate.of(2026, 11, 27));
        // Pascua 2026: 5 de abril, así que el Jueves Santo es el 2.
        assertThat(fechaDe(2026, "Jueves Santo")).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(fechaDe(2027, "Jueves Santo")).isEqualTo(LocalDate.of(2027, 3, 25));
    }

    @Test
    void laIdeaCambiaSegunElRubro() {
        FechaComercial navidad = FechasPeruanas.delAnio(2026).stream()
                .filter(f -> f.nombre().equals("Navidad")).findFirst().orElseThrow();

        assertThat(navidad.ideaPara("Panadería y pastelería")).contains("Panetones");
        assertThat(navidad.ideaPara("Servicios profesionales")).isEqualTo(navidad.idea());
        assertThat(navidad.ideaPara(null)).isEqualTo(navidad.idea());
    }

    @Test
    void elMesTraeSusFechasYUnPlanDeTresPorSemana() throws Exception {
        MesContenidoResponse diciembre = armar(YearMonth.of(2026, 12), LocalDate.of(2026, 12, 1), "Panadería y pastelería");

        List<String> nombres = diciembre.fechas().stream().map(FechaSugerida::nombre).toList();
        assertThat(nombres).contains("Navidad", "Fin de Año", "Año Nuevo"); // Año Nuevo 2027 entra por el margen
        FechaSugerida navidad = diciembre.fechas().stream().filter(f -> f.nombre().equals("Navidad")).findFirst().orElseThrow();
        assertThat(navidad.diasFaltan()).isEqualTo(24);

        // Martes, jueves y sábado, más los días especiales.
        assertThat(diciembre.plan()).isNotEmpty();
        assertThat(diciembre.plan()).allSatisfy(p -> assertThat(
                p.tipo().equals("FECHA_ESPECIAL")
                        || List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY).contains(p.fecha().getDayOfWeek()))
                .isTrue());
        assertThat(diciembre.plan()).anySatisfy(p -> {
            assertThat(p.fecha()).isEqualTo(LocalDate.of(2026, 12, 25));
            assertThat(p.tipo()).isEqualTo("FECHA_ESPECIAL");
        });
    }

    // armar() es de paquete: se llama por reflexión para probarlo con un "hoy" fijo.
    private static MesContenidoResponse armar(YearMonth periodo, LocalDate hoy, String rubro) throws Exception {
        Method metodo = Class.forName("com.adbrand.core.calendario.service.MesContenidoService")
                .getDeclaredMethod("armar", YearMonth.class, LocalDate.class, String.class);
        metodo.setAccessible(true);
        return (MesContenidoResponse) metodo.invoke(null, periodo, hoy, rubro);
    }
}
