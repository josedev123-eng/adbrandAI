from rest_framework import serializers

from .models import Contenido, HistorialContenido, PerfilNegocio

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
            "moderador_id",
            "negocio",
            "fecha_creacion",
        ]

    def get_negocio(self, contenido):
        return _negocio_de(contenido)


class ModerarContenidoSerializer(serializers.Serializer):
    """Serializador para aprobar o rechazar contenido (HU 15)."""

    accion = serializers.ChoiceField(choices=["APROBAR", "RECHAZAR"])
    motivo_rechazo = serializers.CharField(max_length=500, required=False, allow_blank=True)

FORMATO_FECHA = "Usa el formato AAAA-MM-DD."


class FiltrosHistorialSerializer(serializers.Serializer):
    """Filtros opcionales de GET /api/moderacion/historial/ (HU 16, criterio 2)."""

    estado = serializers.ChoiceField(
        choices=["APROBADO", "RECHAZADO"], required=False,
        error_messages={"invalid_choice": "Usa APROBADO o RECHAZADO."},
    )
    desde = serializers.DateField(required=False, input_formats=["%Y-%m-%d"], error_messages={"invalid": FORMATO_FECHA})
    hasta = serializers.DateField(required=False, input_formats=["%Y-%m-%d"], error_messages={"invalid": FORMATO_FECHA})
    pagina = serializers.IntegerField(required=False, min_value=1, default=1)

    def validate(self, datos):
        desde = datos.get("desde")
        hasta = datos.get("hasta")
        if desde and hasta and desde > hasta:
            raise serializers.ValidationError({"desde": "La fecha de inicio no puede ser posterior a la fecha de fin"})
        return datos


class HistorialContenidoSerializer(serializers.ModelSerializer):
    """Una fila del historial: qué se decidió, quién, cuándo y por qué (HU 16, criterio 1)."""

    empresa = serializers.CharField()

    class Meta:
        model = HistorialContenido
        fields = ["id", "empresa", "tipo", "red_social", "oferta", "texto", "estado", "motivo", "moderador", "fecha_decision"]


class PaginaHistorialSerializer(serializers.Serializer):
    """Una página del historial (20 filas)."""

    total = serializers.IntegerField()
    pagina = serializers.IntegerField()
    paginas = serializers.IntegerField()
    resultados = HistorialContenidoSerializer(many=True)
