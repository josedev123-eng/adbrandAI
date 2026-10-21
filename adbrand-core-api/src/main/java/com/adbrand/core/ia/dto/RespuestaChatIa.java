package com.adbrand.core.ia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Lo que responde el servidor de IA. El texto generado viene en choices[0].message.content
// y los tokens usados en usage (HU 17). Algunos servidores no mandan usage: entonces es null.
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaChatIa(List<Opcion> choices, Uso usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(MensajeIa message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Uso(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }

    public String textoGenerado() {
        if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
            return null;
        }
        return choices.get(0).message().content();
    }
}
