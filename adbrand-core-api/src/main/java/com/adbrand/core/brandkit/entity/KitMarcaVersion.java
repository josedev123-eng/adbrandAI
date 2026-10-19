package com.adbrand.core.brandkit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Entidad: una edición guardada del kit de marca (HU 32, tabla kit_marca_version de V17).
@Entity
@Table(name = "kit_marca_version")
public class KitMarcaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kit_marca_id", nullable = false)
    private Long kitMarcaId;

    @Column(name = "paleta_colores", columnDefinition = "TEXT")
    private String paletaColores;

    @Column(name = "tipografia_primaria", length = 200)
    private String tipografiaPrimaria;

    @Column(name = "tipografia_secundaria", length = 200)
    private String tipografiaSecundaria;

    @Column(name = "logo_concepto", columnDefinition = "TEXT")
    private String logoConcepto;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getKitMarcaId() { return kitMarcaId; }
    public void setKitMarcaId(Long kitMarcaId) { this.kitMarcaId = kitMarcaId; }

    public String getPaletaColores() { return paletaColores; }
    public void setPaletaColores(String paletaColores) { this.paletaColores = paletaColores; }

    public String getTipografiaPrimaria() { return tipografiaPrimaria; }
    public void setTipografiaPrimaria(String tipografiaPrimaria) { this.tipografiaPrimaria = tipografiaPrimaria; }

    public String getTipografiaSecundaria() { return tipografiaSecundaria; }
    public void setTipografiaSecundaria(String tipografiaSecundaria) { this.tipografiaSecundaria = tipografiaSecundaria; }

    public String getLogoConcepto() { return logoConcepto; }
    public void setLogoConcepto(String logoConcepto) { this.logoConcepto = logoConcepto; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
