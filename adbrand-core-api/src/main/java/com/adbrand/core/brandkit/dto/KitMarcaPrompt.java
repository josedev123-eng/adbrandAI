package com.adbrand.core.brandkit.dto;

// Los dos mensajes que se envían a la IA: reglas generales (sistema) y el pedido concreto (usuario).
public record KitMarcaPrompt(String sistema, String usuario) {
}