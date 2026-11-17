package com.adbrand.core.notificacion.service;

import com.adbrand.core.notificacion.entity.DispositivoUsuario;
import com.adbrand.core.notificacion.push.EnviadorPush;
import com.adbrand.core.notificacion.repository.DispositivoUsuarioRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// HU 50: punto único de envío. Busca los teléfonos activos del usuario, envía el
// mensaje y borra los tokens que Firebase ya no acepta (app desinstalada, token
// renovado). Los recordatorios de campañas y suscripciones (HU 49 y 52) usan este
// mismo servicio para no repetir la lógica.
@Service
public class ServicioNotificaciones {

    private final DispositivoUsuarioRepository dispositivos;
    private final EnviadorPush enviador;

    public ServicioNotificaciones(DispositivoUsuarioRepository dispositivos, EnviadorPush enviador) {
        this.dispositivos = dispositivos;
        this.enviador = enviador;
    }

    /**
     * Envía un aviso a todos los equipos activos de la empresa.
     *
     * @return cuántos dispositivos recibieron el mensaje.
     */
    @Transactional
    public int enviarAUsuario(Long usuarioId, String titulo, String cuerpo, Map<String, String> datos) {
        List<DispositivoUsuario> activos =
                dispositivos.findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(usuarioId);
        List<String> tokens = activos.stream()
                .map(DispositivoUsuario::getFcmToken)
                .filter(token -> token != null && !token.isBlank())
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            return 0;
        }

        EnviadorPush.ResultadoEnvio resultado = enviador.enviar(tokens, titulo, cuerpo, datos);
        if (!resultado.tokensInvalidos().isEmpty()) {
            dispositivos.deleteByFcmTokenIn(resultado.tokensInvalidos());
        }
        return resultado.enviados();
    }
}
