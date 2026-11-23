package com.adbrand.core.calendario.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// HU 63: fechas del Perú que venden. Las fijas (Fiestas Patrias, Navidad) y las que cambian cada año
// (Día de la Madre: 2.º domingo de mayo; Semana Santa: según la Pascua) se calculan para el año pedido.
// Cada fecha trae una idea de anuncio general y, para los rubros principales, una idea propia del rubro.
public final class FechasPeruanas {

    // clave: parte del nombre del rubro (sin tildes, en minúsculas), igual que en "Mi negocio".
    public record FechaComercial(LocalDate fecha, String nombre, String idea, Map<String, String> ideasPorRubro) {

        public String ideaPara(String rubro) {
            if (rubro != null) {
                String clave = sinTildes(rubro).toLowerCase();
                for (Map.Entry<String, String> entrada : ideasPorRubro.entrySet()) {
                    if (clave.contains(entrada.getKey())) {
                        return entrada.getValue();
                    }
                }
            }
            return idea;
        }
    }

    private FechasPeruanas() {
    }

    // Todas las fechas del año, ordenadas.
    public static List<FechaComercial> delAnio(int anio) {
        List<FechaComercial> fechas = new ArrayList<>();
        LocalDate pascua = pascua(anio);

        fechas.add(fecha(LocalDate.of(anio, 1, 1), "Año Nuevo", "Empieza el año con una oferta de bienvenida.",
                Map.of("belleza", "Año nuevo, look nuevo: descuento en cortes y tintes.",
                        "ropa", "Estrena en Año Nuevo: ropa amarilla con descuento.")));
        fechas.add(fecha(nesimoDia(anio, Month.FEBRUARY, DayOfWeek.SATURDAY, 1), "Día del Pisco Sour",
                "Brinda con lo nuestro: promo por el Día del Pisco Sour.",
                Map.of("restaurante", "2x1 en pisco sour solo este sábado.")));
        fechas.add(fecha(LocalDate.of(anio, 2, 14), "San Valentín", "Regalos y combos para enamorados.",
                Map.of("panader", "Tortas en forma de corazón por encargo.",
                        "restaurante", "Cena para dos con postre de regalo.",
                        "belleza", "Pack de belleza para regalar a tu pareja.")));
        fechas.add(fecha(LocalDate.of(anio, 3, 8), "Día de la Mujer", "Un reconocimiento a ellas: descuento especial.",
                Map.of("belleza", "Día de la Mujer: 20 % en manicure y peinado.")));
        fechas.add(fecha(LocalDate.of(anio, 3, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)).plusWeeks(1),
                "Inicio de clases", "Vuelta al cole: combos para la familia.",
                Map.of("bodega", "Lonchera lista: combo de jugo + galletas + fruta.",
                        "ropa", "Uniformes y mochilas con descuento.",
                        "educaci", "Matrícula abierta: reserva tu vacante.",
                        "panader", "Pan para la lonchera, recién hecho a las 6 am.")));
        fechas.add(fecha(pascua.minusDays(3), "Jueves Santo", "Semana Santa: horarios especiales y platos de temporada.",
                Map.of("restaurante", "Menú de vigilia: pescados y mariscos frescos.",
                        "panader", "Pan de Semana Santa y dulces tradicionales.")));
        fechas.add(fecha(LocalDate.of(anio, 5, 1), "Día del Trabajo", "Un homenaje al que trabaja: promo del feriado.", Map.of()));
        fechas.add(fecha(nesimoDia(anio, Month.MAY, DayOfWeek.SUNDAY, 2), "Día de la Madre",
                "Sorprende a mamá: regalos y combos especiales.",
                Map.of("panader", "Torta para mamá por encargo: reserva hasta el viernes.",
                        "restaurante", "Almuerzo para mamá: postre de regalo.",
                        "belleza", "Mamá se merece un spa: pack con descuento.",
                        "ropa", "El regalo perfecto para mamá con 15 % de descuento.")));
        fechas.add(fecha(nesimoDia(anio, Month.JUNE, DayOfWeek.SUNDAY, 3), "Día del Padre",
                "Para papá: regalos y combos.",
                Map.of("restaurante", "Parrilla para papá: bebida de regalo.",
                        "ropa", "Camisas y polos para papá con descuento.",
                        "tecnolog", "Gadgets para papá a precio especial.")));
        fechas.add(fecha(LocalDate.of(anio, 6, 24), "Inti Raymi / Día del Campesino", "Celebra lo nuestro con productos peruanos.", Map.of()));
        fechas.add(fecha(LocalDate.of(anio, 6, 28), "Día del Ceviche", "El plato bandera en su día: promo especial.",
                Map.of("restaurante", "Ceviche + chicha morada a precio especial.")));
        fechas.add(fecha(nesimoDia(anio, Month.JULY, DayOfWeek.SUNDAY, 3), "Día del Pollo a la Brasa",
                "El favorito del Perú: combo familiar.",
                Map.of("restaurante", "1/4 de pollo + papas + gaseosa a precio de fiesta.",
                        "bodega", "Todo para tu pollada: gaseosas y cremas en combo.")));
        fechas.add(fecha(LocalDate.of(anio, 7, 28), "Fiestas Patrias", "¡Felices Fiestas Patrias! Oferta patriótica.",
                Map.of("panader", "Desayuno patrio: pan con chicharrón para la familia.",
                        "restaurante", "Menú criollo de Fiestas Patrias.",
                        "ropa", "Rojo y blanco con descuento por Fiestas Patrias.",
                        "bodega", "Todo para la reunión patria: combos de bebidas y snacks.")));
        fechas.add(fecha(nesimoDia(anio, Month.AUGUST, DayOfWeek.FRIDAY, 4), "Día del Café Peruano",
                "Café peruano en su día: una taza con promo.",
                Map.of("panader", "Café pasado + pan con queso en combo.",
                        "restaurante", "Café peruano + postre a precio especial.")));
        fechas.add(fecha(LocalDate.of(anio, 8, 30), "Santa Rosa de Lima", "Feriado de Santa Rosa: horarios y promos del día.", Map.of()));
        fechas.add(fecha(nesimoDia(anio, Month.SEPTEMBER, DayOfWeek.SUNDAY, 2), "Día de la Gastronomía Peruana",
                "Lo mejor de nuestra cocina: plato del día especial.",
                Map.of("restaurante", "Festival criollo: lomo saltado, ají de gallina y más.")));
        fechas.add(fecha(LocalDate.of(anio, 9, 23), "Día de la Primavera y la Juventud", "Llegó la primavera: colores y promos frescas.",
                Map.of("ropa", "Nueva colección de primavera.", "belleza", "Look de primavera con descuento.")));
        fechas.add(fecha(LocalDate.of(anio, 10, 16), "Día Mundial del Pan", "Celebra el pan de cada día.",
                Map.of("panader", "Día del Pan: lleva 10 panes y paga 8.")));
        fechas.add(fecha(LocalDate.of(anio, 10, 31), "Día de la Canción Criolla", "Celebra lo criollo: promo con sabor peruano.",
                Map.of("restaurante", "Noche criolla: anticuchos y picarones.",
                        "panader", "Picarones y turrón para la jarana criolla.")));
        fechas.add(fecha(nesimoDia(anio, Month.NOVEMBER, DayOfWeek.THURSDAY, 4).plusDays(1), "Black Friday",
                "Los mejores descuentos del año: solo por hoy.", Map.of()));
        fechas.add(fecha(LocalDate.of(anio, 12, 25), "Navidad", "Navidad en familia: regalos y combos navideños.",
                Map.of("panader", "Panetones y chocolate caliente para la Nochebuena.",
                        "restaurante", "Cena navideña por encargo: pavo con guarniciones.",
                        "ropa", "Regalos de Navidad con envoltura gratis.",
                        "bodega", "Canasta navideña completa.")));
        fechas.add(fecha(LocalDate.of(anio, 12, 31), "Fin de Año", "Despide el año con nosotros.",
                Map.of("ropa", "Ropa amarilla para recibir el año.", "bodega", "Todo para la reunión de Año Nuevo.")));

        fechas.sort(Comparator.comparing(FechaComercial::fecha));
        return fechas;
    }

    // n-ésimo día de la semana del mes (por ejemplo, 2.º domingo de mayo).
    static LocalDate nesimoDia(int anio, Month mes, DayOfWeek dia, int n) {
        return LocalDate.of(anio, mes, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, dia));
    }

    // Domingo de Pascua (algoritmo de Meeus/Jones/Butcher para el calendario gregoriano).
    static LocalDate pascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }

    private static FechaComercial fecha(LocalDate fecha, String nombre, String idea, Map<String, String> porRubro) {
        return new FechaComercial(fecha, nombre, idea, porRubro);
    }

    static String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
