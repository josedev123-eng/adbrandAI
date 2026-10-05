from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny

from . import services
from .models import Rol, UsuarioAdmin
from .serializers import AdministradorSerializer, CrearAdministradorSerializer, DesactivarAdministradorSerializer, RolSerializer


class RolesView(APIView):
    """GET /api/usuarios/roles/ : lista de roles para el formulario."""

    def get(self, request):
        roles = Rol.objects.order_by("id")
        return Response(RolSerializer(roles, many=True).data)


class AdministradoresView(APIView):
    """POST /api/usuarios/administradores/ : crea una cuenta de administrador con su rol."""
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = CrearAdministradorSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        administrador = services.crear_administrador(serializer.validated_data)
        return Response(AdministradorSerializer(administrador).data, status=status.HTTP_201_CREATED)


class DesactivarAdministradorView(APIView):
    """PATCH /api/usuarios/administradores/<id>/desactivar/ : desactiva un administrador (HU 03)."""

    def patch(self, request, admin_id):
        serializer = DesactivarAdministradorSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        if not serializer.validated_data["confirmar"]:
            return Response(
                {"detail": "Debe confirmar la desactivación."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            admin = services.desactivar_administrador(admin_id)
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_400_BAD_REQUEST)

        return Response(AdministradorSerializer(admin).data)