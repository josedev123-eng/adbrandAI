package com.adbrand.core.negocio.entity;

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

// Entidad: representa una fila de la tabla perfil_negocio (creada por Flyway en V2).
@Entity
@Table(name = "perfil_negocio")
public class PerfilNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(name = "nombre_comercial", nullable = false, length = 120)
    private String nombreComercial;

    @Column(nullable = false, length = 80)
    private String rubro;

    @Column(name = "publico_objetivo", nullable = false, length = 300)
    private String publicoObjetivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tono tono;

    @Column(length = 500)
    private String descripcion;

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

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getRubro() { return rubro; }
    public void setRubro(String rubro) { this.rubro = rubro; }

    public String getPublicoObjetivo() { return publicoObjetivo; }
    public void setPublicoObjetivo(String publicoObjetivo) { this.publicoObjetivo = publicoObjetivo; }

    public Tono getTono() { return tono; }
    public void setTono(Tono tono) { this.tono = tono; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}