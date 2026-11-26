package com.adbrand.core.video.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// HU 66: un video con IA pedido a OpenRouter (tabla video_ia, V75).
@Entity
@Table(name = "video_ia")
public class VideoIa {

    public enum Estado { CREANDO, LISTO, FALLIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "contenido_id", nullable = false)
    private Long contenidoId;

    // El id del trabajo en OpenRouter (para preguntar en qué va y descargarlo).
    @Column(name = "trabajo_id", length = 100)
    private String trabajoId;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(nullable = false, length = 20)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Estado estado;

    private Integer progreso;

    @Column(precision = 10, scale = 4)
    private BigDecimal costo;

    @Column(length = 300)
    private String error;

    // El MP4 final (con el titular y la marca). Solo se carga al descargarlo.
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "BYTEA")
    private byte[] video;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public static VideoIa nuevo(Long usuarioId, Long contenidoId, String modelo) {
        VideoIa video = new VideoIa();
        video.usuarioId = usuarioId;
        video.contenidoId = contenidoId;
        video.modelo = modelo;
        video.estado = Estado.CREANDO;
        return video;
    }

    public void listo(byte[] mp4, BigDecimal costoFinal) {
        estado = Estado.LISTO;
        video = mp4;
        costo = costoFinal;
        progreso = 100;
        fechaFin = LocalDateTime.now();
    }

    public void fallido(String motivo, BigDecimal costoFinal) {
        estado = Estado.FALLIDO;
        error = motivo == null ? "No se pudo crear el video." : motivo.substring(0, Math.min(300, motivo.length()));
        costo = costoFinal;
        fechaFin = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getContenidoId() { return contenidoId; }
    public String getTrabajoId() { return trabajoId; }
    public void setTrabajoId(String trabajoId) { this.trabajoId = trabajoId; }
    public String getModelo() { return modelo; }
    public Estado getEstado() { return estado; }
    public Integer getProgreso() { return progreso; }
    public void setProgreso(Integer progreso) { this.progreso = progreso; }
    public BigDecimal getCosto() { return costo; }
    public String getError() { return error; }
    public byte[] getVideo() { return video; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaFin() { return fechaFin; }
}
