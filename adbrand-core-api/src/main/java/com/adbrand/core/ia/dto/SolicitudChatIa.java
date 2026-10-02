package com.adbrand.core.ia.dto;

import java.util.List;

// Lo que se envía al servidor de IA (formato compatible con OpenAI).
public record SolicitudChatIa(String model, List<MensajeIa> messages, boolean stream) {
}
