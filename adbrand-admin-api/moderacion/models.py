from django.db import models


class Contenido(models.Model):
    """Contenido generado por la IA. La tabla la crea Flyway (V3) y la llena Spring (HU 13)."""

    DUDOSO = "DUDOSO"
    APROBADO = "APROBADO"
    RECHAZADO = "RECHAZADO"

    usuario_id = models.BigIntegerField()
    tipo = models.CharField(max_length=20)
    red_social = models.CharField(max_length=20)
    tono = models.CharField(max_length=20)
    oferta = models.CharField(max_length=300)
    texto = models.TextField()
    estado = models.CharField(max_length=20)
    motivo_revision = models.CharField(max_length=500, null=True, blank=True)
    moderador_id = models.BigIntegerField(null=True, blank=True)
    fecha_creacion = models.DateTimeField()
    fecha_actualizacion = models.DateTimeField()

    class Meta:
        managed = False  # la tabla la crea Flyway (V3)
        db_table = "contenido"

    def __str__(self):
        return f"{self.tipo} {self.id} ({self.estado})"


class PerfilNegocio(models.Model):
    """Solo para mostrar a qué negocio pertenece el contenido. La tabla la crea Flyway (V2)."""

    usuario_id = models.BigIntegerField(unique=True)
    nombre_comercial = models.CharField(max_length=120)
    rubro = models.CharField(max_length=80)

    class Meta:
        managed = False  # la tabla la crea Flyway (V2)
        db_table = "perfil_negocio"

    def __str__(self):
        return self.nombre_comercial