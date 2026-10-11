from django.db import models


class AuditoriaAdmin(models.Model):
    """Una acción realizada por un administrador. La tabla la crea Flyway (V9)."""

    DESACTIVAR_ADMIN = "DESACTIVAR_ADMIN"
    CAMBIAR_ROL = "CAMBIAR_ROL"
    APROBAR_CONTENIDO = "APROBAR_CONTENIDO"
    RECHAZAR_CONTENIDO = "RECHAZAR_CONTENIDO"

    admin = models.ForeignKey("usuarios.UsuarioAdmin", on_delete=models.PROTECT, db_column="admin_id")
    accion = models.CharField(max_length=50)
    entidad = models.CharField(max_length=50)
    entidad_id = models.BigIntegerField(null=True, blank=True)
    detalle = models.TextField(null=True, blank=True)
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        managed = False  # la tabla la crea Flyway (V9)
        db_table = "auditoria_admin"

    def __str__(self):
        return f"{self.accion} por admin {self.admin_id}"
