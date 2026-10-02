from rest_framework.response import Response
from rest_framework.views import APIView

from . import services
from .serializers import ContenidoBandejaSerializer, ContenidoDetalleSerializer


class BandejaRevisionView(APIView):
    """GET /api/moderacion/contenidos/dudosos/ : la bandeja de revisión manual."""

    def get(self, request):
        contenidos = services.contenidos_dudosos()
        return Response(ContenidoBandejaSerializer(contenidos, many=True).data)


class DetalleContenidoView(APIView):
    """GET /api/moderacion/contenidos/<id>/ : detalle completo y motivo de la observación."""

    def get(self, request, contenido_id):
        contenido = services.obtener_contenido(contenido_id)
        return Response(ContenidoDetalleSerializer(contenido).data)