from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from . import services
from .serializers import AuditoriaSerializer, FiltrosAuditoriaSerializer


class AuditoriaView(VistaAdmin):
    """GET /api/auditoria/ : historial de acciones de los administradores (HU 04).

    Filtros opcionales: ?admin_id=, ?desde=AAAA-MM-DD y ?hasta=AAAA-MM-DD.
    """

    roles_permitidos = ("SUPERADMIN",)

    def get(self, request):
        filtros = FiltrosAuditoriaSerializer(data=request.query_params)
        filtros.is_valid(raise_exception=True)

        registros = services.historial(
            admin_id=filtros.validated_data.get("admin_id"),
            desde=filtros.validated_data.get("desde"),
            hasta=filtros.validated_data.get("hasta"),
        )

        return Response(AuditoriaSerializer(registros, many=True).data)
