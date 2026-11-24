from rest_framework import status
from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from . import services
from .serializers import EmpresaDetalleSerializer, EmpresaSerializer, SuspenderSerializer

# El superadministrador y el moderador (que ve el contenido prohibido) pueden suspender empresas.
ROLES_EMPRESAS = ("SUPERADMIN", "MODERADOR")


class EmpresasView(VistaAdmin):
    """GET /api/empresas/?busqueda=&estado= : las PYMEs con su plan, consumo y anuncios rechazados."""

    roles_permitidos = ROLES_EMPRESAS

    def get(self, request):
        empresas = services.listar_empresas(
            busqueda=(request.query_params.get("busqueda") or "").strip() or None,
            estado=request.query_params.get("estado"),
        )
        return Response(EmpresaSerializer(empresas, many=True).data)


class SuspenderEmpresaView(VistaAdmin):
    """POST /api/empresas/<id>/suspender/ {motivo} : bloquea el login de la empresa y corta su sesión."""

    roles_permitidos = ROLES_EMPRESAS

    def post(self, request, empresa_id):
        serializer = SuspenderSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            empresa = services.suspender(empresa_id, serializer.validated_data["motivo"], request.user.id)
        except ValueError as error:
            return Response({"detail": str(error)}, status=status.HTTP_400_BAD_REQUEST)
        return Response(EmpresaDetalleSerializer(empresa).data)


class ReactivarEmpresaView(VistaAdmin):
    """POST /api/empresas/<id>/reactivar/ : quita la suspensión."""

    roles_permitidos = ROLES_EMPRESAS

    def post(self, request, empresa_id):
        try:
            empresa = services.reactivar(empresa_id, request.user.id)
        except ValueError as error:
            return Response({"detail": str(error)}, status=status.HTTP_400_BAD_REQUEST)
        return Response(EmpresaDetalleSerializer(empresa).data)
