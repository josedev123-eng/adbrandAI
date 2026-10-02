package com.adbrand.core.revision.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Entidad: una fila de regla_revision (V3). Spring solo la lee; las reglas las administra el admin.
@Entity
@Table(name = "regla_revision")
public class ReglaRevision {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String termino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaRegla categoria;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false)
    private boolean activa;

    protected ReglaRevision() {
    }

    public ReglaRevision(String termino, CategoriaRegla categoria, String motivo) {
        this.termino = termino;
        this.categoria = categoria;
        this.motivo = motivo;
        this.activa = true;
    }

    public Long getId() { return id; }
    public String getTermino() { return termino; }
    public CategoriaRegla getCategoria() { return categoria; }
    public String getMotivo() { return motivo; }
    public boolean isActiva() { return activa; }
}