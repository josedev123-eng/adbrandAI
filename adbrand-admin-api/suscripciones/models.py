from django.db import models


class Suscripcion(models.Model):
    """Suscripción de un cliente. La tabla la crea Flyway (V7)."""

    ACTIVA = "ACTIVA"
    VENCIDA = "VENCIDA"
    PENDIENTE_PAGO = "PENDIENTE_PAGO"
    CANCELADA = "CANCELADA"

    ESTADOS = [
        (ACTIVA, "Activa"),
        (VENCIDA, "Vencida"),
        (PENDIENTE_PAGO, "Pendiente de pago"),
        (CANCELADA, "Cancelada"),
    ]

    usuario_id = models.BigIntegerField()
    cliente = models.CharField(max_length=150)
    plan = models.CharField(max_length=50)
    monto = models.DecimalField(max_digits=10, decimal_places=2)
    estado = models.CharField(max_length=20, choices=ESTADOS)
    fecha_inicio = models.DateField()
    fecha_vencimiento = models.DateField()
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V7)
        db_table = "suscripcion"

    def __str__(self):
        return f"{self.cliente} ({self.estado})"
