from django.db import models


class MensajeContacto(models.Model):
    """Un mensaje del formulario de Contacto de la web. La tabla la crea Flyway (V72)."""

    MOTIVOS = {
        "DUDA": "Tengo una duda",
        "PROBLEMA": "Algo no funciona",
        "PAGOS": "Pagos y planes",
        "SUGERENCIA": "Sugerencia",
        "OTRO": "Otro",
    }

    # null si quien escribió no había iniciado sesión.
    usuario_id = models.BigIntegerField(null=True, blank=True)
    nombre = models.CharField(max_length=100)
    correo = models.CharField(max_length=150)
    asunto = models.CharField(max_length=40)
    mensaje = models.TextField()
    atendido = models.BooleanField(default=False)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V72)
        db_table = "mensaje_contacto"
        ordering = ["atendido", "-fecha_creacion"]

    def __str__(self):
        return f"{self.asunto} de {self.correo}"
