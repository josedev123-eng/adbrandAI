package com.adbrand.core.negocio.dto;

import com.adbrand.core.negocio.entity.Tono;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Lo que llega desde la web al guardar el perfil. Las anotaciones validan cada campo.
public record PerfilNegocioRequest(
        @NotBlank(message = "Ingresa el nombre comercial.")
        @Size(max = 120, message = "Máximo 120 caracteres.")
        String nombreComercial,

        @NotBlank(message = "Selecciona el rubro.")
        @Size(max = 80, message = "Máximo 80 caracteres.")
        String rubro,

        @NotBlank(message = "Describe a tu público objetivo.")
        @Size(max = 300, message = "Máximo 300 caracteres.")
        String publicoObjetivo,

        @NotNull(message = "Selecciona el tono.")
        Tono tono,

        @Size(max = 500, message = "Máximo 500 caracteres.")
        String descripcion) {
}