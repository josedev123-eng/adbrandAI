package com.adbrand.core.ia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Un mensaje de la conversación con la IA. role es "system", "user" o "assistant".
@JsonIgnoreProperties(ignoreUnknown = true)
public record MensajeIa(String role, String content) {
}
