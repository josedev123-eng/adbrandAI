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


class Plan(models.Model):
    """Planes que contrata la PYME (V20). Se lee el límite de tokens del plan para el panel (HU 19)."""

    nombre = models.CharField(max_length=40, unique=True)
    precio = models.DecimalField(max_digits=10, decimal_places=2)
    limite_tokens = models.IntegerField()
    descripcion = models.CharField(max_length=300)
    activo = models.BooleanField(default=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V20)
        db_table = "plan"

    def __str__(self):
        return self.nombre


class Pago(models.Model):
    """Pago recibido por una suscripción. La tabla la crea Flyway (V10)."""

    TARJETA = "TARJETA"
    YAPE = "YAPE"

    METODOS = [
        (TARJETA, "Tarjeta"),
        (YAPE, "Yape"),
    ]

    suscripcion = models.ForeignKey(Suscripcion, on_delete=models.PROTECT, db_column="suscripcion_id")
    monto = models.DecimalField(max_digits=10, decimal_places=2)
    fecha_pago = models.DateField()
    metodo = models.CharField(max_length=20, choices=METODOS)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V10)
        db_table = "pago"

    def __str__(self):
        return f"{self.monto} el {self.fecha_pago}"
