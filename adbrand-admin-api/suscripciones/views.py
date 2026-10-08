from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from . import services
from .serializers import SuscripcionSerializer


class SuscripcionesConDeudaView(APIView):
    """GET /api/suscripciones/con-deuda/ : suscripciones vencidas y pendientes de pago (HU 08)."""

    permission_classes = [AllowAny]

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
