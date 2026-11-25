package com.adbrand.core.notificacion.entity;

// HU 49, 50 y 52: los tres tipos de aviso. El valor viaja en el payload "type" y
// la app lo usa para elegir el canal y la pantalla de destino.
public enum TipoNotificacion {
    SISTEMA("system_alert"),
    CAMPANA("campaign_reminder"),
    SUSCRIPCION("subscription_reminder");

    private final String valor;

    TipoNotificacion(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }

    public static TipoNotificacion desde(String valor) {
        if (valor != null) {
            for (TipoNotificacion tipo : values()) {
                if (tipo.valor.equalsIgnoreCase(valor) || tipo.name().equalsIgnoreCase(valor)) {
                    return tipo;
                }
            }
        }
        return SISTEMA;
    }
}
