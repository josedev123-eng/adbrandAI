from rest_framework import serializers


class ConsumoEmpresaSerializer(serializers.Serializer):
    """Una fila del panel: cuánto gastó una empresa en el mes (HU 17) y si está cerca de su límite (HU 19)."""

    usuario_id = serializers.IntegerField()
    empresa = serializers.CharField()
    tokens = serializers.IntegerField()
    generaciones = serializers.IntegerField()
    ultima_generacion = serializers.DateTimeField()
    # HU 19: null si no hay ningún límite configurado.
    limite = serializers.IntegerField(allow_null=True)
    porcentaje_aviso = serializers.IntegerField(allow_null=True)
    porcentaje = serializers.FloatField(allow_null=True)
    alerta = serializers.BooleanField()


class AlertaConsumoSerializer(serializers.Serializer):
    """Una empresa que llegó al porcentaje de aviso de su límite mensual (HU 19, criterio 1)."""

    usuario_id = serializers.IntegerField()
    empresa = serializers.CharField()
    tokens = serializers.IntegerField()
    limite = serializers.IntegerField()
    porcentaje = serializers.FloatField()


class ConsumoMesSerializer(serializers.Serializer):
    """El panel completo: el mes, el total, el detalle por empresa (HU 17) y las alertas (HU 19)."""

    mes = serializers.CharField()
    total = serializers.IntegerField()
    empresas = ConsumoEmpresaSerializer(many=True)
    alertas = AlertaConsumoSerializer(many=True)
