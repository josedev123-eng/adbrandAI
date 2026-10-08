from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny

from . import services
from .serializers import ContenidoBandejaSerializer, ContenidoDetalleSerializer, ModerarContenidoSerializer


class BandejaRevisionView(APIView):
    """GET /api/moderacion/contenidos/dudosos/ : la bandeja de revisión manual."""
    permission_classes = [AllowAny]

    def get(self, request):
        contenidos = services.contenidos_dudosos()
        return Response(ContenidoBandejaSerializer(contenidos, many=True).data)


class DetalleContenidoView(APIView):
    """GET /api/moderacion/contenidos/<id>/ : detalle completo y motivo de la observación."""
    permission_classes = [AllowAny]

    def get(self, request, contenido_id):
        contenido = services.obtener_contenido(contenido_id)
        return Response(ContenidoDetalleSerializer(contenido).data)


class ModerarContenidoView(APIView):
    """POST /api/moderacion/contenidos/<id>/moderar/ : aprueba o rechaza un contenido (HU 15)."""
    permission_classes = [AllowAny]

    def post(self, request, contenido_id):
        serializer = ModerarContenidoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            contenido = services.moderar_contenido(
                contenido_id=contenido_id,
                accion=serializer.validated_data["accion"],
                moderador_id=serializer.validated_data["moderador_id"],
                motivo_rechazo=serializer.validated_data.get("motivo_rechazo"),
            )
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_400_BAD_REQUEST)

        return Response(ContenidoDetalleSerializer(contenido).data)