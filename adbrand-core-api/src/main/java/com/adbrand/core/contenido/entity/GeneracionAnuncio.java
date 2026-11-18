package com.adbrand.core.contenido.entity;

import com.adbrand.core.contenido.dto.RedSocial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Una vez que la PYME pidió opciones para una oferta (tabla generacion_anuncio, V38).
@Entity
@Table(name = "generacion_anuncio")
public class GeneracionAnuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 300)
    private String oferta;

    @Enumerated(EnumType.STRING)
    @Column(name = "red_social", nullable = false, length = 20)
    private RedSocial redSocial;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getOferta() { return oferta; }
    public void setOferta(String oferta) { this.oferta = oferta; }

    public RedSocial getRedSocial() { return redSocial; }
    public void setRedSocial(RedSocial redSocial) { this.redSocial = redSocial; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
