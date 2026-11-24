package com.adbrand.core.previsualizacion.entity;

import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
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

// HU 47: un anuncio compartido desde la app (tabla anuncio_compartido, V50).
@Entity
@Table(name = "anuncio_compartido")
public class AnuncioCompartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contenido_id", nullable = false)
    private Long contenidoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormatoAnuncio formato;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
    }

    public static AnuncioCompartido nuevo(Long contenidoId, Long usuarioId, FormatoAnuncio formato) {
        AnuncioCompartido registro = new AnuncioCompartido();
        registro.contenidoId = contenidoId;
        registro.usuarioId = usuarioId;
        registro.formato = formato;
        return registro;
    }

    public Long getId() { return id; }
    public Long getContenidoId() { return contenidoId; }
    public Long getUsuarioId() { return usuarioId; }
    public FormatoAnuncio getFormato() { return formato; }
    public OffsetDateTime getFecha() { return fecha; }
}
