package com.adbrand.core.notificacion.push;

import java.util.List;
import java.util.Map;

// HU 50: contrato de envío de push. Permite cambiar Firebase por otro proveedor
// (o por el modo consola de desarrollo) sin tocar los servicios de recordatorios.
public interface EnviadorPush {

    /**
     * Envía el mismo mensaje a todos los tokens indicados.
     *
     * @return cuántos llegaron y qué tokens ya no sirven.
     */
    ResultadoEnvio enviar(List<String> tokens, String titulo, String cuerpo, Map<String, String> datos);

    // HU 50: tokens que Firebase rechazó porque el equipo desinstaló la app o el
    // token se renovó. El servicio los borra para no volver a intentarlo.
    record ResultadoEnvio(int enviados, List<String> tokensInvalidos) {

        public static ResultadoEnvio vacio() {
            return new ResultadoEnvio(0, List.of());
        }
    }
}
