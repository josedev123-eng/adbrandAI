from rest_framework import serializers

from . import services
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


FORMATO_MES = "Usa el formato AAAA-MM."


class RangoDeMesesSerializer(serializers.Serializer):
    """Filtro opcional de ingresos-mensuales/: ?desde=AAAA-MM&hasta=AAAA-MM (HU 07)."""

    desde = serializers.DateField(required=False, input_formats=["%Y-%m"], error_messages={"invalid": FORMATO_MES})
    hasta = serializers.DateField(required=False, input_formats=["%Y-%m"], error_messages={"invalid": FORMATO_MES})

    def validate(self, datos):
        desde = datos.get("desde")
        hasta = datos.get("hasta")

        # Sin rango se devuelven los últimos 6 meses; con rango hacen falta los dos extremos.
        if desde is None and hasta is None:
            return datos
        if desde is None:
            raise serializers.ValidationError({"desde": "Indica también el mes inicial."})
        if hasta is None:
            raise serializers.ValidationError({"hasta": "Indica también el mes final."})

        if desde > hasta:
            raise serializers.ValidationError({"desde": "El mes inicial no puede ser mayor que el final."})
        if services.meses_entre(desde, hasta) > services.MAXIMO_MESES:
            raise serializers.ValidationError(
                {"hasta": f"El rango no puede pasar de {services.MAXIMO_MESES} meses."}
            )

        return datos
