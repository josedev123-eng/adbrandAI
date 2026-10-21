from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from . import services
from .serializers import ConsumoMesSerializer


class ConsumoActualView(VistaAdmin):
    """GET /api/tokens/consumo-actual/ : tokens de IA consumidos por cada empresa en el mes actual (HU 17)."""

    roles_permitidos = ("SUPERADMIN", "TECNICO")

    def get(self, request):
        return Response(ConsumoMesSerializer(services.consumo_del_mes_actual()).data)
