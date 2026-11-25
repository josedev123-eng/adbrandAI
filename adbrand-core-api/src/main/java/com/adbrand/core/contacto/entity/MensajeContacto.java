package com.adbrand.core.contacto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Entidad: un mensaje del formulario de Contacto (tabla mensaje_contacto, V72).
@Entity
@Table(name = "mensaje_contacto")
public class MensajeContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null si quien escribió no había iniciado sesión.
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    // DUDA, PROBLEMA, PAGOS, SUGERENCIA u OTRO.
    @Column(nullable = false, length = 40)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    private boolean atendido;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public static MensajeContacto nuevo(Long usuarioId, String nombre, String correo, String asunto, String mensaje) {
        MensajeContacto nuevo = new MensajeContacto();
        nuevo.usuarioId = usuarioId;
        nuevo.nombre = nombre;
        nuevo.correo = correo;
        nuevo.asunto = asunto;
        nuevo.mensaje = mensaje;
        return nuevo;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public boolean isAtendido() { return atendido; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
