package com.adbrand.core.ia.service;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.client.ClienteIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// Único punto de entrada a la IA para todo el proyecto (ver CONTEXTO.md).
// Con AI_MOCK=true devuelve un texto de prueba para poder trabajar fuera de Tecsup.
@Service
public class ServicioIa {

    private static final Logger log = LoggerFactory.getLogger(ServicioIa.class);

    private final ClienteIa cliente;
    private final IaProperties ia;

    public ServicioIa(ClienteIa cliente, IaProperties ia) {
        this.cliente = cliente;
        this.ia = ia;
    }

    public boolean esSimulado() {
        return ia.mock();
    }

    public String generarTexto(String sistema, String usuario) {
        if (ia.mock()) {
            return respuestaSimulada(usuario);
        }
        try {
            String texto = cliente.completar(sistema, usuario);
            if (texto == null || texto.isBlank()) {
                throw new IaNoDisponibleException("La IA respondió sin texto", null);
            }
            return texto.trim();
        } catch (RestClientException ex) {
            log.warn("Falló la llamada al servidor de IA: {}", ex.getMessage());
            throw new IaNoDisponibleException("No se pudo conectar con la IA", ex);
        }
    }

    // Arma un anuncio de ejemplo usando la oferta que vino en el prompt.
    private String respuestaSimulada(String usuario) {
        String oferta = usuario.lines()
                .filter(linea -> linea.startsWith("Oferta a anunciar: "))
                .map(linea -> linea.substring("Oferta a anunciar: ".length()))
                .findFirst()
                .orElse("nuestra promoción de hoy");
        return """
                ¡Esto no te lo puedes perder! %s.
                Ven a visitarnos y aprovecha antes de que se acabe.
                #Promo #ComproLocal #Peru""".formatted(oferta);
    }
}