from django.db import models


class VerificacionServidor(models.Model):
    """Una verificación del estado de un servicio (HU 18). La tabla la crea Flyway (V33)."""

    BACKEND = "BACKEND"
    BASE_DATOS = "BASE_DATOS"
    IA = "IA"
    ACTIVO = "ACTIVO"
    CAIDO = "CAIDO"

    servicio = models.CharField(max_length=20)
    estado = models.CharField(max_length=10)
    tiempo_ms = models.IntegerField(null=True, blank=True)
    detalle = models.CharField(max_length=255, null=True, blank=True)
    fecha = models.DateTimeField()

    class Meta:
        managed = False  # la tabla la crea Flyway (V33)
        db_table = "verificacion_servidor"

    def __str__(self):
        return f"{self.servicio} {self.estado} ({self.fecha:%Y-%m-%d %H:%M:%S})"
