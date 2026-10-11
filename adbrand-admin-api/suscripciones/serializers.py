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


class IngresoMensualSerializer(serializers.Serializer):
    """Una fila de ingresos: el mes como AAAA-MM y lo cobrado con dos decimales (HU 06)."""

    mes = serializers.CharField()
    total = serializers.DecimalField(max_digits=12, decimal_places=2)
