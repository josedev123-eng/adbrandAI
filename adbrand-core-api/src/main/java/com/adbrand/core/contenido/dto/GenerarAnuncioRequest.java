package com.adbrand.core.contenido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Lo que envía la web para pedir un anuncio.
public record GenerarAnuncioRequest(
        @NotBlank(message = "Escribe la oferta que quieres anunciar.")
        @Size(max = 300, message = "Máximo 300 caracteres.")
        String oferta,

        @NotNull(message = "Selecciona la red social.")
        RedSocial redSocial,

        // Festividad elegida en el calendario ("Halloween", "Navidad"…): el texto y la imagen se ambientan en ella.
        // Llega vacía cuando el anuncio se crea desde "Crear anuncio" directamente.
        @Size(max = 80, message = "Máximo 80 caracteres.")
        String ocasion) {

    public GenerarAnuncioRequest(String oferta, RedSocial redSocial) {
        this(oferta, redSocial, null);
    }

    public boolean tieneOcasion() {
        return ocasion != null && !ocasion.isBlank();
    }
}