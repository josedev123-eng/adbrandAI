package com.adbrand.core.notificacion.recordatorio;

import com.adbrand.core.calendario.entity.Campana;

// HU 49: una campaña con las fechas clave que caen mañana. Una campaña puede
// empezar y terminar el mismo día: el aviso lo explica en una sola frase.
public record CampanaPorRecordar(
        Campana campana,
        boolean empiezaManana,
        boolean terminaManana) {

    public String mensaje() {
        if (empiezaManana && terminaManana) {
            return "«" + campana.getNombre() + "» empieza y termina mañana.";
        }
        if (empiezaManana) {
            return "«" + campana.getNombre() + "» empieza mañana.";
        }
        return "«" + campana.getNombre() + "» termina mañana.";
    }
}
