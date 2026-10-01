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
    nombres = models.CharField(max_length=100)
    apellidos = models.CharField(max_length=100)
    correo = models.CharField(max_length=150, unique=True)
    contrasena = models.CharField(max_length=255)
    rol = models.ForeignKey(Rol, on_delete=models.PROTECT, db_column="rol_id")
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V1)
        db_table = "usuario_admin"

    def __str__(self):
        return self.correo
