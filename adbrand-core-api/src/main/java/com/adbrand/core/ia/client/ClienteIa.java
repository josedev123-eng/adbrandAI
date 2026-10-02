package com.adbrand.core.ia.client;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.dto.MensajeIa;
import com.adbrand.core.ia.dto.RespuestaChatIa;
import com.adbrand.core.ia.dto.SolicitudChatIa;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Client: solo hace la llamada HTTP al servidor de IA del instituto. No decide nada más.
@Component
public class ClienteIa {

    private final RestClient restClientIa;
    private final IaProperties ia;

    public ClienteIa(RestClient restClientIa, IaProperties ia) {
        this.restClientIa = restClientIa;
        this.ia = ia;
    }

    public String completar(String sistema, String usuario) {
        SolicitudChatIa solicitud = new SolicitudChatIa(
                ia.model(),
                List.of(new MensajeIa("system", sistema), new MensajeIa("user", usuario)),
                false);

        RespuestaChatIa respuesta = restClientIa.post()
                .uri(ia.chatPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(RespuestaChatIa.class);

        return respuesta == null ? null : respuesta.textoGenerado();
    }
}