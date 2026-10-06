package com.adbrand.core.ia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Lo que responde el servidor de IA. El texto generado viene en choices[0].message.content.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaChatIa(List<Opcion> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(MensajeIa message) {
    }

    public String textoGenerado() {
        if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
            return null;
        }
        return choices.get(0).message().content();
    }
}