package com.adbrand.core.pago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// HU 26 con Mercado Pago: la web ya convirtió la tarjeta en un token con la llave pública.
// El número completo y el CVV nunca llegan a nuestro servidor.
public record PagoMercadoPagoRequest(
        @NotNull(message = "Selecciona un plan.")
        Long planId,

        @NotBlank(message = "Falta el token de la tarjeta.")
        @Size(max = 100, message = "El token de la tarjeta no es válido.")
        String token,

        // payment_method_id de Mercado Pago: visa, master, amex...
        @NotBlank(message = "Falta el tipo de tarjeta.")
        @Pattern(regexp = "[a-z_]{2,30}", message = "El tipo de tarjeta no es válido.")
        String metodoPago,

        // issuer_id: el banco que emitió la tarjeta. Puede venir vacío.
        @Pattern(regexp = "^$|[0-9]{1,10}", message = "El banco de la tarjeta no es válido.")
        String emisorId,

        @NotBlank(message = "Escribe tu correo.")
        @Email(message = "Escribe un correo válido.")
        String correo,

        // Opcionales: Mercado Pago devuelve el titular y los últimos dígitos en el pago.
        // Si no vienen ahí, se usan estos para el comprobante (HU 28).
        @Size(max = 120, message = "El nombre del titular tiene hasta 120 caracteres.")
        String titular,

        @Pattern(regexp = "^$|[0-9]{4}", message = "Los últimos dígitos no son válidos.")
        String ultimosDigitos,

        // Para la API de Orders: credit_card, debit_card o prepaid_card. Si no viene, se usa credit_card.
        @Pattern(regexp = "^$|credit_card|debit_card|prepaid_card", message = "El tipo de tarjeta no es válido.")
        String tipoTarjeta) {
}
