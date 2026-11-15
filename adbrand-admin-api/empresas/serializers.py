from rest_framework import serializers

from .models import Empresa


class EmpresaSerializer(serializers.ModelSerializer):
    """Una PYME en la lista del administrador, con lo que ayuda a detectar abusos."""

    plan = serializers.CharField(read_only=True, allow_null=True)
    tokens_mes = serializers.IntegerField(read_only=True)
    anuncios = serializers.IntegerField(read_only=True)
    rechazados = serializers.IntegerField(read_only=True)

    class Meta:
        model = Empresa
        fields = [
            "id", "nombre", "nombre_empresa", "correo", "estado", "motivo_suspension", "fecha_suspension",
            "fecha_creacion", "plan", "tokens_mes", "anuncios", "rechazados",
        ]


class EmpresaDetalleSerializer(serializers.ModelSerializer):
    """La empresa después de suspenderla o reactivarla."""

    class Meta:
        model = Empresa
        fields = ["id", "nombre_empresa", "correo", "estado", "motivo_suspension", "fecha_suspension"]


class SuspenderSerializer(serializers.Serializer):
    motivo = serializers.CharField(
        min_length=10, max_length=300,
        error_messages={
            "required": "Escribe el motivo de la suspensión.",
            "blank": "Escribe el motivo de la suspensión.",
            "min_length": "Explica el motivo en al menos 10 caracteres.",
            "max_length": "Máximo 300 caracteres.",
        },
    )
