package com.adbrand.core.contenido.entity;

import com.adbrand.core.imagen.dto.ImagenGenerada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Una versión de la imagen de la opción elegida (tabla version_anuncio, V39). v1 es la original.
@Entity
@Table(name = "version_anuncio")
public class VersionAnuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcion_id", nullable = false)
    private OpcionAnuncio opcion;

    @Column(nullable = false)
    private Integer numero;

    @Column(length = 300)
    private String instruccion;

    @Column(name = "imagen_tipo", nullable = false, length = 40)
    private String imagenTipo;

    @Column(name = "imagen_base64", nullable = false, columnDefinition = "TEXT")
    private String imagenBase64;

    @Column(name = "proveedor_imagen", nullable = false, length = 20)
    private String proveedorImagen;

    @Column(name = "imagen_simulada", nullable = false)
    private boolean imagenSimulada;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }

    public static VersionAnuncio de(OpcionAnuncio opcion, int numero, String instruccion, ImagenGenerada imagen) {
        VersionAnuncio version = new VersionAnuncio();
        version.opcion = opcion;
        version.numero = numero;
        version.instruccion = instruccion;
        version.imagenTipo = imagen.tipo();
        version.imagenBase64 = imagen.base64();
        version.proveedorImagen = imagen.proveedor();
        version.imagenSimulada = imagen.simulado();
        return version;
    }

    public ImagenGenerada imagen() {
        return new ImagenGenerada(imagenTipo, imagenBase64, proveedorImagen, imagenSimulada);
    }

    public Long getId() { return id; }
    public OpcionAnuncio getOpcion() { return opcion; }
    public Integer getNumero() { return numero; }
    public String getInstruccion() { return instruccion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
