package com.adbrand.core.ia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Límite mensual de tokens (V31). Lo edita el administrador técnico en Django (HU 19).
// usuario_id NULL = límite por defecto, que funciona como el plan gratuito (V43). Spring solo lo lee.
@Entity
@Table(name = "limite_tokens")
public class LimiteTokens {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "limite_mensual", nullable = false)
    private long limiteMensual;

    @Column(name = "porcentaje_aviso", nullable = false)
    private int porcentajeAviso;

    public Long getId() { return id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public long getLimiteMensual() { return limiteMensual; }
    public void setLimiteMensual(long limiteMensual) { this.limiteMensual = limiteMensual; }

    public int getPorcentajeAviso() { return porcentajeAviso; }
    public void setPorcentajeAviso(int porcentajeAviso) { this.porcentajeAviso = porcentajeAviso; }
}
