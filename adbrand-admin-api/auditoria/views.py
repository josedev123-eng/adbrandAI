from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from . import services
from .serializers import AuditoriaSerializer, FiltrosAuditoriaSerializer


class AuditoriaView(APIView):
    """GET /api/auditoria/ : historial de acciones de los administradores (HU 04).

    Filtros opcionales: ?admin_id=, ?desde=AAAA-MM-DD y ?hasta=AAAA-MM-DD.
    """

    permission_classes = [AllowAny]

    def get(self, request):
        filtros = FiltrosAuditoriaSerializer(data=request.query_params)
        filtros.is_valid(raise_exception=True)

        registros = services.historial(
            admin_id=filtros.validated_data.get("admin_id"),
            desde=filtros.validated_data.get("desde"),
            hasta=filtros.validated_data.get("hasta"),
        )

        return Response(AuditoriaSerializer(registros, many=True).data)
