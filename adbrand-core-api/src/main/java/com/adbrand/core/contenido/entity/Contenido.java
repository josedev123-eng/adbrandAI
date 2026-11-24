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

    // HU 53 y 54: lo que decidió el dueño desde el móvil (V41).
    @Enumerated(EnumType.STRING)
    @Column(name = "decision_dueno", nullable = false, length = 20)
    private DecisionDueno decisionDueno = DecisionDueno.PENDIENTE;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    // HU 54: motivo opcional del dueño al rechazar (V48).
    @Column(name = "motivo_dueno", length = 300)
    private String motivoDueno;

    // HU 15: quién moderó el contenido (lo escribe Django) y si el dueño todavía no vio esa decisión (V44).
    @Column(name = "moderador_id")
    private Long moderadorId;

    @Column(name = "aviso_pendiente", nullable = false)
    private boolean avisoPendiente;

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

    public DecisionDueno getDecisionDueno() { return decisionDueno; }
    public void setDecisionDueno(DecisionDueno decisionDueno) { this.decisionDueno = decisionDueno; }

    public LocalDateTime getFechaDecision() { return fechaDecision; }

    public Long getModeradorId() { return moderadorId; }
    public void setModeradorId(Long moderadorId) { this.moderadorId = moderadorId; }

    public boolean isAvisoPendiente() { return avisoPendiente; }
    public void setAvisoPendiente(boolean avisoPendiente) { this.avisoPendiente = avisoPendiente; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaDecision(LocalDateTime fechaDecision) { this.fechaDecision = fechaDecision; }
    public String getMotivoDueno() { return motivoDueno; }
    public void setMotivoDueno(String motivoDueno) { this.motivoDueno = motivoDueno; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}