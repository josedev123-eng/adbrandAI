package com.adbrand.core.contenido.entity;

import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.negocio.entity.Tono;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Entidad: una fila de la tabla contenido (V3). Guarda cada anuncio con el resultado del filtro (HU 13).
@Entity
@Table(name = "contenido")
public class Contenido {

    // Por ahora solo hay anuncios; reels y calendario usarán otros tipos más adelante.
    public static final String TIPO_ANUNCIO = "ANUNCIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "red_social", nullable = false, length = 20)
    private RedSocial redSocial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tono tono;

    @Column(nullable = false, length = 300)
    private String oferta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContenido estado;

    @Column(name = "motivo_revision", length = 500)
    private String motivoRevision;

    @Column(name = "prompt_original", columnDefinition = "TEXT")
    private String promptOriginal;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public RedSocial getRedSocial() { return redSocial; }
    public void setRedSocial(RedSocial redSocial) { this.redSocial = redSocial; }

    public Tono getTono() { return tono; }
    public void setTono(Tono tono) { this.tono = tono; }

    public String getOferta() { return oferta; }
    public void setOferta(String oferta) { this.oferta = oferta; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public EstadoContenido getEstado() { return estado; }
    public void setEstado(EstadoContenido estado) { this.estado = estado; }

    public String getMotivoRevision() { return motivoRevision; }
    public void setMotivoRevision(String motivoRevision) { this.motivoRevision = motivoRevision; }

    public String getPromptOriginal() { return promptOriginal; }
    public void setPromptOriginal(String promptOriginal) { this.promptOriginal = promptOriginal; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}