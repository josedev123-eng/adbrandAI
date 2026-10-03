package com.adbrand.core.brandkit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Entidad: Kit de Marca generado para un negocio (HU 11).
@Entity
@Table(name = "kit_marca")
public class KitMarca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "nombre_negocio", nullable = false, length = 120)
    private String nombreNegocio;

    @Column(name = "sector", nullable = false, length = 80)
    private String sector;

    @Column(name = "paleta_sugerida", length = 300)
    private String paletaSugerida;

    @Column(name = "valores_eslogan", length = 500)
    private String valoresEslogan;

    @Column(name = "estilo_visual", nullable = false, length = 100)
    private String estiloVisual;

    @Column(name = "logo_concepto", columnDefinition = "TEXT")
    private String logoConcepto;

    @Column(name = "tipografia_primaria", length = 200)
    private String tipografiaPrimaria;

    @Column(name = "tipografia_secundaria", length = 200)
    private String tipografiaSecundaria;

    @Column(name = "paleta_colores", columnDefinition = "TEXT")
    private String paletaColores;

    @Column(name = "voz_marca", columnDefinition = "TEXT")
    private String vozMarca;

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

    public String getNombreNegocio() { return nombreNegocio; }
    public void setNombreNegocio(String nombreNegocio) { this.nombreNegocio = nombreNegocio; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getPaletaSugerida() { return paletaSugerida; }
    public void setPaletaSugerida(String paletaSugerida) { this.paletaSugerida = paletaSugerida; }

    public String getValoresEslogan() { return valoresEslogan; }
    public void setValoresEslogan(String valoresEslogan) { this.valoresEslogan = valoresEslogan; }

    public String getEstiloVisual() { return estiloVisual; }
    public void setEstiloVisual(String estiloVisual) { this.estiloVisual = estiloVisual; }

    public String getLogoConcepto() { return logoConcepto; }
    public void setLogoConcepto(String logoConcepto) { this.logoConcepto = logoConcepto; }

    public String getTipografiaPrimaria() { return tipografiaPrimaria; }
    public void setTipografiaPrimaria(String tipografiaPrimaria) { this.tipografiaPrimaria = tipografiaPrimaria; }

    public String getTipografiaSecundaria() { return tipografiaSecundaria; }
    public void setTipografiaSecundaria(String tipografiaSecundaria) { this.tipografiaSecundaria = tipografiaSecundaria; }

    public String getPaletaColores() { return paletaColores; }
    public void setPaletaColores(String paletaColores) { this.paletaColores = paletaColores; }

    public String getVozMarca() { return vozMarca; }
    public void setVozMarca(String vozMarca) { this.vozMarca = vozMarca; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}