from rest_framework import serializers

from . import services


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


class ImagenesEmpresaSerializer(serializers.Serializer):
    """Imágenes con IA de una empresa en el mes: borradores (gratis) y retoques (de pago)."""

    usuario_id = serializers.IntegerField()
    empresa = serializers.CharField()
    borradores = serializers.IntegerField()
    retoques = serializers.IntegerField()
    videos = serializers.IntegerField(default=0)  # HU 66
    costo_videos = serializers.FloatField(default=0)
    ultima = serializers.DateTimeField()
    en_limite = serializers.BooleanField()


class ImagenesMesSerializer(serializers.Serializer):
    """Control de gasto de las IA de imágenes en el mes actual."""

    borradores = serializers.IntegerField()
    retoques = serializers.IntegerField()
    videos = serializers.IntegerField(default=0)  # HU 66
    costo_videos = serializers.FloatField(default=0)
    limite_retoques = serializers.IntegerField()
    empresas = ImagenesEmpresaSerializer(many=True)


class ConsumoMesSerializer(serializers.Serializer):
    """El panel completo: el mes, el total, el detalle por empresa (HU 17) y las alertas (HU 19)."""

    mes = serializers.CharField()
    total = serializers.IntegerField()
    empresas = ConsumoEmpresaSerializer(many=True)
    alertas = AlertaConsumoSerializer(many=True)
    imagenes = ImagenesMesSerializer()


FORMATO_MES = "Usa el formato AAAA-MM."


class PeriodoSerializer(serializers.Serializer):
    """Filtro de historial/: ?desde=AAAA-MM&hasta=AAAA-MM, los dos obligatorios; ?usuario_id= opcional (HU 20)."""

    desde = serializers.DateField(
        input_formats=["%Y-%m"],
        error_messages={"invalid": FORMATO_MES, "required": "Indica el mes de inicio."},
    )
    hasta = serializers.DateField(
        input_formats=["%Y-%m"],
        error_messages={"invalid": FORMATO_MES, "required": "Indica el mes de fin."},
    )
    usuario_id = serializers.IntegerField(required=False, min_value=1)

    def validate(self, datos):
        # HU 20, criterio 2: un periodo al revés no se consulta.
        if datos["desde"] > datos["hasta"]:
            raise serializers.ValidationError({"desde": "La fecha de inicio no puede ser posterior a la fecha de fin"})
        if services.meses_entre(datos["desde"], datos["hasta"]) > services.MAXIMO_MESES:
            raise serializers.ValidationError({"hasta": f"El periodo no puede pasar de {services.MAXIMO_MESES} meses."})
        return datos


class ConsumoDelMesSerializer(serializers.Serializer):
    """Un mes del historial (HU 20)."""

    mes = serializers.CharField()
    tokens = serializers.IntegerField()
    generaciones = serializers.IntegerField()


class HistorialSerializer(serializers.Serializer):
    """El historial completo del periodo: cada mes y el total (HU 20, criterio 1)."""

    desde = serializers.CharField()
    hasta = serializers.CharField()
    total = serializers.IntegerField()
    meses = ConsumoDelMesSerializer(many=True)
