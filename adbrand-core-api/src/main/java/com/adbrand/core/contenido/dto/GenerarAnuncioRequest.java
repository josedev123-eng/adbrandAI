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
        String ocasion,

        // Foto del producto que subió el dueño (data URL JPG o PNG, opcional). Si viene, las 4 opciones usan esa
        // foto en vez de una imagen inventada por la IA, así el anuncio muestra lo que de verdad vende.
        @Size(max = 8_000_000, message = "La foto es muy pesada. Usa una de menos de 5 MB.")
        String foto) {

    public GenerarAnuncioRequest(String oferta, RedSocial redSocial) {
        this(oferta, redSocial, null, null);
    }

    public GenerarAnuncioRequest(String oferta, RedSocial redSocial, String ocasion) {
        this(oferta, redSocial, ocasion, null);
    }

    public boolean tieneFoto() {
        return foto != null && !foto.isBlank();
    }

    public boolean tieneOcasion() {
        return ocasion != null && !ocasion.isBlank();
    }
}