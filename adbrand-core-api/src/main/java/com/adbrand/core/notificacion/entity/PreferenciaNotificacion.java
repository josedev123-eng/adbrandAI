package com.adbrand.core.notificacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// HU 51: qué avisos quiere recibir cada empresa. La fila se crea la primera vez
// que la app cambia una preferencia; mientras no exista, todo está activo.
@Entity
@Table(name = "notification_preferences")
public class PreferenciaNotificacion {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private boolean campanas = true;

    @Column(nullable = false)
    private boolean suscripciones = true;

    @Column(nullable = false)
    private boolean novedades = true;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public PreferenciaNotificacion() {
    }

    public PreferenciaNotificacion(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    @PrePersist
    @PreUpdate
    void marcarCambio() {
        this.actualizadoEn = LocalDateTime.now();
    }

    /** HU 51: los avisos del sistema siempre llegan; los demás respetan la preferencia. */
    public boolean permite(TipoNotificacion tipo) {
        return switch (tipo) {
            case CAMPANA -> campanas;
            case SUSCRIPCION -> suscripciones;
            case SISTEMA -> true;
        };
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public boolean isCampanas() { return campanas; }
    public void setCampanas(boolean campanas) { this.campanas = campanas; }

    public boolean isSuscripciones() { return suscripciones; }
    public void setSuscripciones(boolean suscripciones) { this.suscripciones = suscripciones; }

    public boolean isNovedades() { return novedades; }
    public void setNovedades(boolean novedades) { this.novedades = novedades; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
