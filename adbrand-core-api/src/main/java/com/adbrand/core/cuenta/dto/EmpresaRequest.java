package com.adbrand.core.cuenta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Lo que envía la web al guardar los datos de la empresa (HU 23).
public record EmpresaRequest(
        @NotBlank(message = "Ingresa el RUC.")
        @Pattern(regexp = "^(10|20)\\d{9}$", message = "El RUC tiene 11 dígitos y empieza en 10 o 20.")
        String ruc,

        @NotBlank(message = "Ingresa la razón social.")
        @Size(max = 200, message = "Máximo 200 caracteres.")
        String razonSocial,

        // Opcional: celular de 9 dígitos que empieza en 9, o fijo de 7 dígitos.
        @Pattern(regexp = "^(9\\d{8}|\\d{7})$",
                message = "Escribe un celular de 9 dígitos que empiece en 9, o un fijo de 7 dígitos.")
        String telefono,

        @NotBlank(message = "Ingresa la dirección.")
        @Size(max = 250, message = "Máximo 250 caracteres.")
        String direccion) {

    // Quita los espacios de los extremos; un teléfono vacío se guarda como "sin teléfono".
    public EmpresaRequest {
        ruc = ruc == null ? null : ruc.trim();
        razonSocial = razonSocial == null ? null : razonSocial.trim();
        telefono = telefono == null || telefono.isBlank() ? null : telefono.trim();
        direccion = direccion == null ? null : direccion.trim();
    }
}
