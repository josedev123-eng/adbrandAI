package com.adbrand.core.imagen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

// Una imagen generada con IA, para el control de gasto (tabla consumo_imagen, V40). La consulta Django en el admin.
@Entity
@Table(name = "consumo_imagen")
public class ConsumoImagen {

    // BORRADOR: las 4 opciones del anuncio (IA gratis). RETOQUE: editar la opción elegida (IA de pago).
    public enum Tipo { BORRADOR, RETOQUE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, length = 20)
    private String proveedor;

    @Column(nullable = false)
    private boolean simulado;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @PrePersist
    void alCrear() {
        fecha = OffsetDateTime.now();
    }

    public static ConsumoImagen de(Long usuarioId, Tipo tipo, String proveedor, boolean simulado) {
        ConsumoImagen consumo = new ConsumoImagen();
        consumo.usuarioId = usuarioId;
        consumo.tipo = tipo;
        consumo.proveedor = proveedor;
        consumo.simulado = simulado;
        return consumo;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Tipo getTipo() { return tipo; }
    public String getProveedor() { return proveedor; }
    public boolean isSimulado() { return simulado; }
    public OffsetDateTime getFecha() { return fecha; }
}
