from rest_framework import status
from rest_framework.response import Response

from auditoria.services import registrar_accion
from usuarios.vistas import VistaAdmin

from . import services
from .serializers import ContenidoBandejaSerializer, ContenidoDetalleSerializer, ModerarContenidoSerializer


class BandejaRevisionView(VistaAdmin):
    """GET /api/moderacion/contenidos/dudosos/ : la bandeja de revisión manual."""
    roles_permitidos = ("SUPERADMIN", "MODERADOR")

    def get(self, request):
        contenidos = services.contenidos_dudosos()
        return Response(ContenidoBandejaSerializer(contenidos, many=True).data)


class DetalleContenidoView(VistaAdmin):
    """GET /api/moderacion/contenidos/<id>/ : detalle completo y motivo de la observación."""
    roles_permitidos = ("SUPERADMIN", "MODERADOR")

    def get(self, request, contenido_id):
        contenido = services.obtener_contenido(contenido_id)
        return Response(ContenidoDetalleSerializer(contenido).data)


class ModerarContenidoView(VistaAdmin):
    """POST /api/moderacion/contenidos/<id>/moderar/ : aprueba o rechaza un contenido (HU 15)."""
    roles_permitidos = ("SUPERADMIN", "MODERADOR")

    def post(self, request, contenido_id):
        serializer = ModerarContenidoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            contenido = services.moderar_contenido(
                contenido_id=contenido_id,
                accion=serializer.validated_data["accion"],
                moderador_id=request.user.id,
                motivo_rechazo=serializer.validated_data.get("motivo_rechazo"),
            )
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_400_BAD_REQUEST)

        registrar_accion(contenido.moderador_id, f"{serializer.validated_data['accion']}_CONTENIDO", "contenido", contenido.id, contenido.oferta)

        return Response(ContenidoDetalleSerializer(contenido).data)