from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from . import services
from .serializers import EstadoServidorSerializer


class EstadoServidorView(VistaAdmin):
    """GET /api/servidor/estado/ : verifica ahora el backend, la base de datos y la IA (HU 18)."""

    roles_permitidos = ("SUPERADMIN", "TECNICO")

    def get(self, request):
        return Response(EstadoServidorSerializer(services.verificar_estado()).data)
