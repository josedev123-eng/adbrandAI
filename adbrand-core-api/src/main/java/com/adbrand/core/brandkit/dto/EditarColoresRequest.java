package com.adbrand.core.brandkit.dto;

import com.adbrand.core.brandkit.dto.EditarKitMarcaRequest.ColorKit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// HU 30: lo que envía el editor de colores. Solo cambia la paleta; el resto del kit queda igual.
// Cada color usa las mismas reglas que la HU 32 (#RRGGBB y un nombre opcional).
public record EditarColoresRequest(
        @NotEmpty(message = "Agrega al menos un color.")
        @Size(max = 8, message = "Máximo 8 colores.")
        List<@Valid ColorKit> colores) {
}
