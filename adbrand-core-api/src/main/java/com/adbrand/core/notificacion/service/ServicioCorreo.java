package com.adbrand.core.notificacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envía correos a los usuarios PYME (HU 24; Edu lo reutiliza en la HU 28).
// Con correo.mock=true (MAIL_MOCK) solo escribe el correo en la consola, para trabajar sin servidor SMTP.
// Con false lo envía por SMTP con los datos spring.mail.* del .env.
@Service
public class ServicioCorreo {

    private static final Logger log = LoggerFactory.getLogger(ServicioCorreo.class);

    private final boolean simulado;
    private final String remitente;
    private final ObjectProvider<JavaMailSender> enviador;

    public ServicioCorreo(@Value("${correo.mock:true}") boolean simulado,
                          @Value("${correo.remitente:no-responder@adbrand.ai}") String remitente,
                          ObjectProvider<JavaMailSender> enviador) {
        this.simulado = simulado;
        this.remitente = remitente;
        this.enviador = enviador;
    }

    public boolean esSimulado() {
        return simulado;
    }

    public void enviar(String destino, String asunto, String texto) {
        if (simulado) {
            log.info("[CORREO SIMULADO] Para: {} | Asunto: {}\n{}", destino, asunto, texto);
            return;
        }

        JavaMailSender correo = enviador.getIfAvailable();
        if (correo == null) {
            throw new IllegalStateException("No hay servidor de correo configurado (spring.mail.host).");
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        correo.send(mensaje);
    }
}
