from django.db import models


class Empresa(models.Model):
    """La cuenta de una PYME (tabla usuario de Spring, HU 21; V45 agrega la suspensión).

    El administrador la puede suspender si abusa: el estado INACTIVO bloquea el login en Spring
    y corta la sesión abierta. Django solo lee y cambia estos campos.
    """

    ACTIVO = "ACTIVO"
    INACTIVO = "INACTIVO"

    nombre = models.CharField(max_length=100)
    nombre_empresa = models.CharField(max_length=150)
    correo = models.CharField(max_length=150)
    estado = models.CharField(max_length=20, default=ACTIVO)
    motivo_suspension = models.CharField(max_length=300, null=True, blank=True)
    fecha_suspension = models.DateTimeField(null=True, blank=True)
    fecha_creacion = models.DateTimeField(null=True, blank=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V13)
        db_table = "usuario"

    def __str__(self):
        return self.nombre_empresa
