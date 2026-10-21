from rest_framework import serializers


class ConsumoEmpresaSerializer(serializers.Serializer):
    """Una fila del panel: cuánto gastó una empresa en el mes (HU 17)."""

    usuario_id = serializers.IntegerField()
    empresa = serializers.CharField()
    tokens = serializers.IntegerField()
    generaciones = serializers.IntegerField()
    ultima_generacion = serializers.DateTimeField()


class ConsumoMesSerializer(serializers.Serializer):
    """El panel completo: el mes, el total y el detalle por empresa (HU 17)."""

    mes = serializers.CharField()
    total = serializers.IntegerField()
    empresas = ConsumoEmpresaSerializer(many=True)
