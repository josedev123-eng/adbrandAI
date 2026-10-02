from rest_framework import serializers

from .models import Contenido, PerfilNegocio

LARGO_EXTRACTO = 120


def _negocio_de(contenido):
    perfil = PerfilNegocio.objects.filter(usuario_id=contenido.usuario_id).first()
    return {"nombre": perfil.nombre_comercial, "rubro": perfil.rubro} if perfil else None


class ContenidoBandejaSerializer(serializers.ModelSerializer):
    """Una fila de la bandeja: lo justo para reconocer el contenido."""

    extracto = serializers.SerializerMethodField()

    class Meta:
        model = Contenido
        fields = ["id", "tipo", "red_social", "oferta", "extracto", "motivo_revision", "fecha_creacion"]

    def get_extracto(self, contenido):
        texto = " ".join(contenido.texto.split())
        return texto if len(texto) <= LARGO_EXTRACTO else texto[:LARGO_EXTRACTO].rstrip() + "..."


class ContenidoDetalleSerializer(serializers.ModelSerializer):
    """El detalle completo con el motivo de la observación (HU 14, criterio 2)."""

    negocio = serializers.SerializerMethodField()

    class Meta:
        model = Contenido
        fields = [
            "id",
            "tipo",
            "estado",
            "red_social",
            "tono",
            "oferta",
            "texto",
            "motivo_revision",
            "negocio",
            "fecha_creacion",
        ]

    def get_negocio(self, contenido):
        return _negocio_de(contenido)