from rest_framework import serializers

from .models import Suscripcion


class SuscripcionSerializer(serializers.ModelSerializer):
    """Lo que la API devuelve. La web usa exactamente estos nombres de campo."""

    class Meta:
        model = Suscripcion
        fields = [
            "id",
            "usuario_id",
            "cliente",
            "plan",
            "monto",
            "estado",
            "fecha_inicio",
            "fecha_vencimiento",
            "fecha_creacion",
        ]
