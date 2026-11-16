package com.adbrand.core.notificacion.entity;

// HU 50: sistema operativo del teléfono que recibe los avisos. La tabla guarda
// "android" o "ios" en minúsculas (CHECK de V38), por eso el converter.
public enum Plataforma {
    ANDROID,
    IOS;

    public String valor() {
        return name().toLowerCase();
    }

    public static Plataforma desde(String valor) {
        if (valor == null) {
            return ANDROID;
        }
        for (Plataforma plataforma : values()) {
            if (plataforma.name().equalsIgnoreCase(valor)) {
                return plataforma;
            }
        }
        return ANDROID;
    }
}
