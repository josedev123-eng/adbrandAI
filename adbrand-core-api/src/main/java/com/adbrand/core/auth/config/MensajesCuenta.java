package com.adbrand.core.auth.config;

// Mensaje que ve una empresa suspendida por el administrador: al iniciar sesión y si ya tenía la sesión abierta.
public final class MensajesCuenta {

    private MensajesCuenta() {
    }

    public static String suspendida(String motivo) {
        String base = "Tu cuenta fue suspendida por el equipo de AdBrand.AI.";
        String conMotivo = motivo == null || motivo.isBlank() ? base : base + " Motivo: " + motivo.trim();
        return conMotivo + " Escribe a soporte para reactivarla.";
    }
}
