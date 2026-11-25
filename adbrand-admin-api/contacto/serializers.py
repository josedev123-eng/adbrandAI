from rest_framework import serializers

from .models import MensajeContacto


class MensajeContactoSerializer(serializers.ModelSerializer):
    """Un mensaje con el nombre del motivo para mostrarlo tal cual."""

    motivo = serializers.SerializerMethodField()

    class Meta:
        model = MensajeContacto
        fields = ["id", "usuario_id", "nombre", "correo", "asunto", "motivo", "mensaje", "atendido", "fecha_creacion"]

    def get_motivo(self, mensaje):
        return MensajeContacto.MOTIVOS.get(mensaje.asunto, mensaje.asunto)


class AtendidoSerializer(serializers.Serializer):
    atendido = serializers.BooleanField()
