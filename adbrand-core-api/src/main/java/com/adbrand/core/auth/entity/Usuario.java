package com.adbrand.core.auth.entity;

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

// Entidad: una cuenta de usuario PYME (tabla usuario, creada por Flyway en V13).
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_empresa", nullable = false, length = 150)
    private String nombreEmpresa;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    // Solo el hash BCrypt. La contraseña nunca se guarda ni se devuelve.
    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    // Si el administrador suspendió la cuenta (estado INACTIVO): el porqué y cuándo (V45). Lo escribe Django.
    @Column(name = "motivo_suspension", length = 300)
    private String motivoSuspension;

    @Column(name = "fecha_suspension")
    private java.time.OffsetDateTime fechaSuspension;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // HU 22: último inicio de sesión (V14).
    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    // HU 23: datos de la empresa (V16). Opcionales hasta que el usuario los guarda en "Mi cuenta".
    @Column(unique = true, length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(length = 15)
    private String telefono;

    @Column(length = 250)
    private String direccion;

    // Color del avatar elegido en "Mi cuenta" (V42). null = automático según el nombre.
    @Column(name = "color_avatar", length = 20)
    private String colorAvatar;

    // App móvil: id de la cuenta en Firebase Authentication (V49). null si nunca entró con Firebase.
    @Column(name = "firebase_uid", length = 128, unique = true)
    private String firebaseUid;

    // Si ya vio o saltó el tutorial guiado de la web (V46).
    @Column(name = "tutorial_visto", nullable = false)
    private boolean tutorialVisto;

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

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }

    public String getMotivoSuspension() { return motivoSuspension; }
    public void setMotivoSuspension(String motivoSuspension) { this.motivoSuspension = motivoSuspension; }

    public java.time.OffsetDateTime getFechaSuspension() { return fechaSuspension; }
    public void setFechaSuspension(java.time.OffsetDateTime fechaSuspension) { this.fechaSuspension = fechaSuspension; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getColorAvatar() { return colorAvatar; }
    public void setColorAvatar(String colorAvatar) { this.colorAvatar = colorAvatar; }
    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }
    public boolean isTutorialVisto() { return tutorialVisto; }
    public void setTutorialVisto(boolean tutorialVisto) { this.tutorialVisto = tutorialVisto; }
}
