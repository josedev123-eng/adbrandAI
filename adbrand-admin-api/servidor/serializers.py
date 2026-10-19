from rest_framework import serializers


class EstadoServicioSerializer(serializers.Serializer):
    """El estado de un servicio: BACKEND, BASE_DATOS o IA (HU 18)."""

    servicio = serializers.CharField()
    nombre = serializers.CharField()
    estado = serializers.CharField()  # ACTIVO o CAIDO
    tiempo_ms = serializers.IntegerField(allow_null=True)
    detalle = serializers.CharField(allow_null=True)


class EstadoServidorSerializer(serializers.Serializer):
    """El panel completo: cuándo se verificó y el estado de cada servicio (HU 18, criterio 1)."""

    verificado_en = serializers.CharField()
    servicios = EstadoServicioSerializer(many=True)
