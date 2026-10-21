package com.adbrand.core.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// HU 26: datos que manda la web para pagar con tarjeta. El CVV nunca se guarda.
public record PagoTarjetaRequest(
        @NotNull(message = "Selecciona un plan.")
        Long planId,

        @NotBlank(message = "Escribe el número de la tarjeta.")
        @Pattern(regexp = "[0-9 ]{13,23}", message = "El número de la tarjeta tiene entre 13 y 19 dígitos.")
        String numeroTarjeta,

        @NotBlank(message = "Escribe el nombre del titular.")
        @Size(min = 5, max = 120, message = "El nombre del titular tiene entre 5 y 120 caracteres.")
        String titular,

        @NotBlank(message = "Escribe el vencimiento (MM/AA).")
        @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}", message = "Usa el formato MM/AA.")
        String vencimiento,

        @NotBlank(message = "Escribe el CVV.")
        @Pattern(regexp = "[0-9]{3,4}", message = "El CVV tiene 3 o 4 dígitos.")
        String cvv) {
}
