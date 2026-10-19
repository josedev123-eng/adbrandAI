from rest_framework import serializers

from .models import AuditoriaAdmin

FORMATO_FECHA = "Usa el formato AAAA-MM-DD."


class FiltrosAuditoriaSerializer(serializers.Serializer):
    """Filtros opcionales de GET /api/auditoria/."""

    admin_id = serializers.IntegerField(min_value=1, required=False)
    desde = serializers.DateField(required=False, error_messages={"invalid": FORMATO_FECHA})
    hasta = serializers.DateField(required=False, error_messages={"invalid": FORMATO_FECHA})

    def validate(self, datos):
        desde = datos.get("desde")
        hasta = datos.get("hasta")
        if desde and hasta and desde > hasta:
            raise serializers.ValidationError({"desde": "La fecha inicial no puede ser mayor que la final."})
        return datos


class AuditoriaSerializer(serializers.ModelSerializer):
    """Una fila del historial, con el nombre y el correo del administrador."""

    admin = serializers.SerializerMethodField()

    class Meta:
        model = AuditoriaAdmin
        fields = ["id", "admin", "accion", "entidad", "entidad_id", "detalle", "fecha"]

    def get_admin(self, registro):
        admin = registro.admin
        return {
            "id": admin.id,
            "nombre": f"{admin.nombres} {admin.apellidos}",
            "correo": admin.correo,
        }
