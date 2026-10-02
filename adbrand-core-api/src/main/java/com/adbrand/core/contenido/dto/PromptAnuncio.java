package com.adbrand.core.contenido.dto;

// Los dos mensajes que se envían a la IA: las reglas generales (sistema) y el pedido concreto (usuario).
public record PromptAnuncio(String sistema, String usuario) {
}