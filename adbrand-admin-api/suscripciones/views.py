from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from . import services
from .serializers import IngresoMensualSerializer, RangoDeMesesSerializer, SuscripcionSerializer


class SuscripcionesConDeudaView(VistaAdmin):
    """GET /api/suscripciones/con-deuda/ : suscripciones vencidas y pendientes de pago (HU 08)."""

    roles_permitidos = ("SUPERADMIN", "FINANZAS")

    def get(self, request):
        estado = request.query_params.get("estado")

        if estado is not None and estado not in services.ESTADOS_ACEPTADOS:
            return Response(
                {"detail": "Estado inválido. Use VENCIDA o PENDIENTE_PAGO."},
                status=400,
            )

        try:
            suscripciones = services.suscripciones_con_deuda(estado)
        except ValueError as e:
            return Response({"detail": str(e)}, status=400)

        return Response(SuscripcionSerializer(suscripciones, many=True).data)


class SuscripcionesActivasView(VistaAdmin):
    """GET /api/suscripciones/activas/ : número de suscripciones activas hoy (HU 05)."""

    roles_permitidos = ("SUPERADMIN", "FINANZAS")

    def get(self, request):
        return Response({"total": services.total_suscripciones_activas()})


class IngresosMensualesView(VistaAdmin):
    """GET /api/suscripciones/ingresos-mensuales/ : ingresos de los últimos 6 meses (HU 06).

    Con ?desde=AAAA-MM&hasta=AAAA-MM devuelve ese rango de meses, hasta 24 (HU 07).
    """

    roles_permitidos = ("SUPERADMIN", "FINANZAS")

    def get(self, request):
        rango = RangoDeMesesSerializer(data=request.query_params)
        rango.is_valid(raise_exception=True)

        ingresos = services.ingresos_mensuales(
            desde=rango.validated_data.get("desde"),
            hasta=rango.validated_data.get("hasta"),
        )

        return Response(IngresoMensualSerializer(ingresos, many=True).data)
