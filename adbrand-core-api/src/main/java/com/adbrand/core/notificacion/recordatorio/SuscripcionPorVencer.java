package com.adbrand.core.notificacion.recordatorio;

import com.adbrand.core.pago.entity.Suscripcion;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// HU 52: una suscripción que vence dentro de los próximos 3 días. El mensaje
// cambia según se renueve sola o la PYME tenga que pagar a mano.
public record SuscripcionPorVencer(Suscripcion suscripcion, long diasRestantes) {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-PE"));

    public boolean renovacionAutomatica() {
        return suscripcion.isRenovacionAutomatica();
    }

    public String modo() {
        return renovacionAutomatica() ? "automatica" : "manual";
    }

    public String titulo() {
        return renovacionAutomatica()
                ? "Tu plan se renueva en " + diasTexto()
                : "Tu plan vence en " + diasTexto();
    }

    public String mensaje() {
        String plan = suscripcion.getPlan();
        String fecha = suscripcion.getFechaVencimiento().format(FECHA);
        if (renovacionAutomatica()) {
            return "El plan " + plan + " se renueva automáticamente el " + fecha
                    + ". Revisa tu método de pago para no interrumpir tus campañas.";
        }
        return "El plan " + plan + " vence el " + fecha
                + ". Renueva antes para seguir publicando sin interrupciones.";
    }

    private String diasTexto() {
        return switch ((int) diasRestantes) {
            case 0 -> "hoy";
            case 1 -> "1 día";
            default -> diasRestantes + " días";
        };
    }
}
