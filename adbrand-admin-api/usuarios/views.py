from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny

from . import services
from .authentication import AdminAuthentication
from .models import Rol, UsuarioAdmin
from .permissions import IsAdminActivo
from .serializers import AdministradorSerializer, CrearAdministradorSerializer, DesactivarAdministradorSerializer, RolSerializer


class RolesView(APIView):
    """GET /api/usuarios/roles/ : lista de roles para el formulario."""

    def get(self, request):
        roles = Rol.objects.order_by("id")
        return Response(RolSerializer(roles, many=True).data)


class AdministradoresView(APIView):
    """GET /api/usuarios/administradores/ : lista las cuentas, nunca la contraseña (HU 03).
    POST /api/usuarios/administradores/ : crea una cuenta de administrador con su rol (HU 01)."""
    permission_classes = [AllowAny]

    def get(self, request):
        administradores = UsuarioAdmin.objects.select_related("rol").order_by("id")
        return Response(AdministradorSerializer(administradores, many=True).data)

    def post(self, request):
        serializer = CrearAdministradorSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        administrador = services.crear_administrador(serializer.validated_data)
        return Response(AdministradorSerializer(administrador).data, status=status.HTTP_201_CREATED)


class DesactivarAdministradorView(APIView):
    """PATCH /api/usuarios/administradores/<id>/desactivar/ : desactiva un administrador (HU 03).

    Solo un superadministrador activo puede usarlo, y nunca sobre su propia cuenta.
    """
    authentication_classes = [AdminAuthentication]
    permission_classes = [IsAdminActivo]

    def patch(self, request, admin_id):
        if getattr(request.user.rol, "nombre", None) != "SUPERADMIN":
            return Response(
                {"detail": "Solo un superadministrador puede desactivar cuentas."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if request.user.id == admin_id:
            return Response(
                {"detail": "No puedes desactivar tu propia cuenta."},
                status=status.HTTP_403_FORBIDDEN,
            )

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