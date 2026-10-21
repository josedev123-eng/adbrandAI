from django.db import models


class ConsumoToken(models.Model):
    """Tokens de IA que gastó una empresa en una generación (HU 17). La tabla la crea Flyway (V30) y la llena Spring."""

    usuario_id = models.BigIntegerField()  # la empresa que pidió la generación
    origen = models.CharField(max_length=30)  # ANUNCIO, REGENERAR_ANUNCIO o KIT_MARCA
    modelo = models.CharField(max_length=120, null=True, blank=True)
    tokens_entrada = models.IntegerField(default=0)
    tokens_salida = models.IntegerField(default=0)
    tokens_total = models.IntegerField()
    simulado = models.BooleanField(default=False)
    fecha = models.DateTimeField()

    class Meta:
        managed = False  # la tabla la crea Flyway (V30)
        db_table = "consumo_token"

    def __str__(self):
        return f"Empresa {self.usuario_id}: {self.tokens_total} tokens ({self.origen})"
