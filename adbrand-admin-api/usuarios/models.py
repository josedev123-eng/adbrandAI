from django.db import models


class Rol(models.Model):
    nombre = models.CharField(max_length=50, unique=True)
    descripcion = models.CharField(max_length=200, null=True, blank=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V1)
        db_table = "rol"

    def __str__(self):
        return self.nombre


class UsuarioAdmin(models.Model):
    ACTIVO = "ACTIVO"
    INACTIVO = "INACTIVO"

    nombres = models.CharField(max_length=100)
    apellidos = models.CharField(max_length=100)
    correo = models.CharField(max_length=150, unique=True)
    contrasena = models.CharField(max_length=255)
    rol = models.ForeignKey(Rol, on_delete=models.PROTECT, db_column="rol_id")
    estado = models.CharField(max_length=20, choices=[(ACTIVO, "Activo"), (INACTIVO, "Inactivo")], default=ACTIVO)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    # Bloqueo por intentos fallidos (V54): contraseñas equivocadas seguidas y hasta cuándo está bloqueado.
    intentos_fallidos = models.IntegerField(default=0)
    bloqueado_hasta = models.DateTimeField(null=True, blank=True)
    # Contraseña temporal (V55): la cuenta la creó un superadmin y debe cambiarla al entrar por primera vez.
    debe_cambiar_contrasena = models.BooleanField(default=False)

    class Meta:
        managed = False  # la tabla la crea Flyway (V1)
        db_table = "usuario_admin"

    def __str__(self):
        return self.correo


class CodigoRecuperacionAdmin(models.Model):
    """Código de 6 dígitos para recuperar la contraseña de un administrador (V47).

    Se guarda cifrado; vence a los 15 minutos, sirve una sola vez y se bloquea tras 5 intentos.
    """

    admin = models.ForeignKey(UsuarioAdmin, on_delete=models.CASCADE, db_column="admin_id")
    codigo_hash = models.CharField(max_length=255)
    expira_en = models.DateTimeField()
    usado = models.BooleanField(default=False)
    intentos = models.IntegerField(default=0)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V47)
        db_table = "codigo_recuperacion_admin"
