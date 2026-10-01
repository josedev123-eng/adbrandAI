from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from . import services
from .models import Rol
from .serializers import AdministradorSerializer, CrearAdministradorSerializer, RolSerializer


class RolesView(APIView):
    """GET /api/usuarios/roles/ : lista de roles para el formulario."""

    def get(self, request):
        roles = Rol.objects.order_by("id")
        return Response(RolSerializer(roles, many=True).data)


class AdministradoresView(APIView):
    """POST /api/usuarios/administradores/ : crea una cuenta de administrador con su rol."""

    def post(self, request):
        serializer = CrearAdministradorSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        administrador = services.crear_administrador(serializer.validated_data)
        return Response(AdministradorSerializer(administrador).data, status=status.HTTP_201_CREATED)