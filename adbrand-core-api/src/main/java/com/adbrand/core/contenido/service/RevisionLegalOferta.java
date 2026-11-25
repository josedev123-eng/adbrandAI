package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.ObservacionLegal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// HU 64: "Tu oferta, sin multas". Revisa el texto de un anuncio con las reglas básicas de publicidad del
// Código de Protección y Defensa del Consumidor (Ley 29571) que más fallan las PYMEs en sus promociones:
// decir hasta cuándo vale y cuántas unidades hay, no prometer lo que no se puede probar y mostrar el precio total.
// Son recomendaciones para evitar problemas, no una asesoría legal. No usa IA: no gasta tokens.
public final class RevisionLegalOferta {

    private static final Pattern ES_PROMOCION = Pattern.compile(
            "\\b(promo|promocion|oferta|descuento|dscto|rebaja|liquidacion|remate|2x1|3x2|2 x 1|3 x 2|gratis|regalo|"
                    + "de regalo|por solo|a solo|precio especial|ahorra)\\b|\\d+\\s?%");
    private static final Pattern TIENE_VIGENCIA = Pattern.compile(
            "\\b(hasta el|hasta|valido|vigencia|vigente|solo hoy|solo por hoy|este fin de semana|del \\d|al \\d|"
                    + "lunes|martes|miercoles|jueves|viernes|sabado|domingo|enero|febrero|marzo|abril|mayo|junio|julio|"
                    + "agosto|septiembre|setiembre|octubre|noviembre|diciembre)\\b|\\d{1,2}/\\d{1,2}");
    private static final Pattern TIENE_STOCK = Pattern.compile(
            "\\b(stock|unidades|hasta agotar|agotar existencias|cupos|limitad[oa]s?)\\b");
    private static final Pattern SUPERLATIVO = Pattern.compile(
            "\\b(el mejor|la mejor|los mejores|las mejores|el mas barato|la mas barata|numero 1|n[°º] ?1|"
                    + "nro\\.? ?1|unico en el peru|100 ?% garantizado|garantia total|el unico|la unica)\\b");
    private static final Pattern TIENE_PRECIO = Pattern.compile("s/\\.?\\s?\\d");
    private static final Pattern PRECIO_PARCIAL = Pattern.compile("(\\bmas igv|\\+\\s?igv|\\bsin igv|\\bno incluye igv)\\b");

    private RevisionLegalOferta() {
    }

    public static List<ObservacionLegal> revisar(String oferta, String texto) {
        String todo = normalizar((oferta == null ? "" : oferta) + " " + (texto == null ? "" : texto));
        List<ObservacionLegal> observaciones = new ArrayList<>();
        boolean promocion = ES_PROMOCION.matcher(todo).find();

        if (promocion && !TIENE_VIGENCIA.matcher(todo).find()) {
            observaciones.add(new ObservacionLegal("FALTA_VIGENCIA",
                    "Di hasta cuándo vale la promoción",
                    "Toda promoción debe indicar su duración. Agrega, por ejemplo: \"Válido hasta el 24/12\"."));
        }
        if (promocion && !TIENE_STOCK.matcher(todo).find()) {
            observaciones.add(new ObservacionLegal("FALTA_STOCK",
                    "Indica cuántas unidades hay",
                    "En las promociones se debe informar el número mínimo de unidades disponibles. "
                            + "Agrega, por ejemplo: \"Stock: 50 unidades\" o \"hasta agotar stock (50 unidades)\"."));
        }
        if (SUPERLATIVO.matcher(todo).find()) {
            observaciones.add(new ObservacionLegal("AFIRMACION_SIN_RESPALDO",
                    "Cuidado con \"el mejor\" o \"el más barato\"",
                    "La publicidad debe ser veraz: si dices que eres el mejor, el único o el más barato, debes poder "
                            + "probarlo. Mejor di lo que te hace especial: \"pan recién horneado a las 6 am\"."));
        }
        if (TIENE_PRECIO.matcher(todo).find() && PRECIO_PARCIAL.matcher(todo).find()) {
            observaciones.add(new ObservacionLegal("PRECIO_SIN_IMPUESTOS",
                    "Muestra el precio final",
                    "El precio que anuncias debe ser el total que paga el cliente, con IGV incluido."));
        }
        return observaciones;
    }

    // Sin tildes y en minúsculas, para que "Válido" y "valido" cuenten igual.
    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
