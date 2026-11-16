package com.adbrand.core.notificacion.config;

import com.adbrand.core.notificacion.push.EnviadorPush;
import com.adbrand.core.notificacion.push.EnviadorPushConsola;
import com.adbrand.core.notificacion.push.EnviadorPushFirebase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// HU 50: elige el enviador de push. Con FIREBASE_ENABLED=true usa Firebase Admin;
// sin credenciales (o en desarrollo) escribe los avisos en la consola.
@Configuration
public class NotificacionConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConfig.class);

    @Bean
    public EnviadorPush enviadorPush(
            @Value("${notificaciones.firebase.habilitado:false}") boolean habilitado,
            @Value("${notificaciones.firebase.credenciales:}") String credenciales,
            @Value("${notificaciones.firebase.proyecto:}") String proyecto) {
        if (habilitado) {
            log.info("Notificaciones push con Firebase (proyecto={}).", proyecto.isBlank() ? "auto" : proyecto);
            return new EnviadorPushFirebase(credenciales, proyecto);
        }
        log.info("Notificaciones push en modo consola. Define FIREBASE_ENABLED=true para usar Firebase.");
        return new EnviadorPushConsola();
    }
}
