package com.adbrand.core.notificacion.push;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// HU 50: modo de desarrollo. Sin credenciales de Firebase el envío se escribe en
// la consola, así se puede probar el flujo completo (recordatorios, preferencias)
// sin quemar cuota ni configurar un proyecto real.
public class EnviadorPushConsola implements EnviadorPush {

    private static final Logger log = LoggerFactory.getLogger(EnviadorPushConsola.class);

    @Override
    public ResultadoEnvio enviar(List<String> tokens, String titulo, String cuerpo, Map<String, String> datos) {
        if (tokens == null || tokens.isEmpty()) {
            return EnviadorPush.ResultadoEnvio.vacio();
        }
        log.info("[push:consola] {} dispositivo(s) | {} | {} | datos={}", tokens.size(), titulo, cuerpo, datos);
        return new EnviadorPush.ResultadoEnvio(tokens.size(), List.of());
    }
}
