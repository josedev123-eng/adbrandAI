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
import jakarta.persistence.Table;

// Una de las 4 opciones de una generación: su texto y su imagen (tabla opcion_anuncio, V38).
@Entity
@Table(name = "opcion_anuncio")
public class OpcionAnuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generacion_id", nullable = false)
    private GeneracionAnuncio generacion;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 40)
    private String estilo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "texto_simulado", nullable = false)
    private boolean textoSimulado;

    @Column(name = "imagen_tipo", nullable = false, length = 40)
    private String imagenTipo;

    @Column(name = "imagen_base64", nullable = false, columnDefinition = "TEXT")
    private String imagenBase64;

    @Column(name = "proveedor_imagen", nullable = false, length = 20)
    private String proveedorImagen;

    @Column(name = "imagen_simulada", nullable = false)
    private boolean imagenSimulada;

    @Column(nullable = false)
    private boolean elegida;

    @Column(name = "contenido_id")
    private Long contenidoId;

    // V39: qué versión de la imagen se está usando (null = la original, antes de cualquier retoque).
    @Column(name = "version_actual")
    private Integer versionActual;

    // La imagen guardada, en el mismo formato que devuelven las IA.
    public ImagenGenerada imagen() {
        return new ImagenGenerada(imagenTipo, imagenBase64, proveedorImagen, imagenSimulada);
    }

    public void ponerImagen(ImagenGenerada imagen) {
        this.imagenTipo = imagen.tipo();
        this.imagenBase64 = imagen.base64();
        this.proveedorImagen = imagen.proveedor();
        this.imagenSimulada = imagen.simulado();
    }

    public Long getId() { return id; }

    public GeneracionAnuncio getGeneracion() { return generacion; }
    public void setGeneracion(GeneracionAnuncio generacion) { this.generacion = generacion; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getEstilo() { return estilo; }
    public void setEstilo(String estilo) { this.estilo = estilo; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    // HU 67: titular corto (3 a 6 palabras) escrito aparte por la IA; null si no vino.
    @Column(length = 80)
    private String titular;

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public boolean isTextoSimulado() { return textoSimulado; }
    public void setTextoSimulado(boolean textoSimulado) { this.textoSimulado = textoSimulado; }

    public boolean isElegida() { return elegida; }
    public void setElegida(boolean elegida) { this.elegida = elegida; }

    public Long getContenidoId() { return contenidoId; }
    public void setContenidoId(Long contenidoId) { this.contenidoId = contenidoId; }

    public Integer getVersionActual() { return versionActual; }
    public void setVersionActual(Integer versionActual) { this.versionActual = versionActual; }
}
