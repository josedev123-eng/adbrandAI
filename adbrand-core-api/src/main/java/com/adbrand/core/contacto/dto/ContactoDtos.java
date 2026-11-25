package com.adbrand.core.contacto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

// Formulario de Contacto: lo que manda la web y lo que responde.
public final class ContactoDtos {

    private ContactoDtos() {
    }

    public record EnviarMensajeRequest(
            @NotBlank(message = "Escribe tu nombre.")
            @Size(max = 100, message = "Máximo 100 caracteres.")
            String nombre,

            @NotBlank(message = "Escribe tu correo para poder responderte.")
            @Email(message = "Escribe un correo válido.")
            @Size(max = 150, message = "Máximo 150 caracteres.")
            String correo,

            @NotBlank(message = "Elige el motivo.")
            @Pattern(regexp = "DUDA|PROBLEMA|PAGOS|SUGERENCIA|OTRO", message = "Elige un motivo de la lista.")
            String asunto,

            @NotBlank(message = "Escribe tu mensaje.")
            @Size(min = 10, max = 2000, message = "Escribe entre 10 y 2000 caracteres.")
            String mensaje) {
    }

    public record MensajeEnviadoResponse(Long id, LocalDateTime fecha) {
    }
}
