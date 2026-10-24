from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from . import services
from .serializers import ConsumoMesSerializer, HistorialSerializer, PeriodoSerializer


class ConsumoActualView(VistaAdmin):
    """GET /api/tokens/consumo-actual/ : tokens de IA consumidos por cada empresa en el mes actual (HU 17)."""

    roles_permitidos = ("SUPERADMIN", "TECNICO")

    def get(self, request):
        return Response(ConsumoMesSerializer(services.consumo_del_mes_actual()).data)


class HistorialView(VistaAdmin):
    """GET /api/tokens/historial/?desde=AAAA-MM&hasta=AAAA-MM : tokens de cada mes del periodo y el total (HU 20).

    ?usuario_id= opcional para ver una sola empresa.
    """

    roles_permitidos = ("SUPERADMIN", "TECNICO")

    def get(self, request):
        periodo = PeriodoSerializer(data=request.query_params)
        periodo.is_valid(raise_exception=True)

        historial = services.historial_por_periodo(
            desde=periodo.validated_data["desde"],
            hasta=periodo.validated_data["hasta"],
            usuario_id=periodo.validated_data.get("usuario_id"),
        )

        return Response(HistorialSerializer(historial).data)
