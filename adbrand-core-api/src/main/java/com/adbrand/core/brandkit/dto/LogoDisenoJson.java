package com.adbrand.core.brandkit.dto;

import tools.jackson.databind.ObjectMapper;

// HU 29: pasa el diseño del logo a JSON para guardarlo en la columna logo_diseno (V36), y de vuelta.
public final class LogoDisenoJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private LogoDisenoJson() {
    }

    public static String aTexto(LogoDiseno diseno) {
        return diseno == null ? null : JSON.writeValueAsString(diseno);
    }

    // Si la columna está vacía (kits anteriores a la HU 29), no hay diseño.
    public static LogoDiseno desdeTexto(String texto) {
        return texto == null || texto.isBlank() ? null : JSON.readValue(texto, LogoDiseno.class);
    }
}
