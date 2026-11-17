from rest_framework.exceptions import NotAuthenticated
from rest_framework.views import APIView

from .authentication import AdminAuthentication
from .permissions import IsAdminActivo, RolPermitido, SinContrasenaTemporal


class VistaAdmin(APIView):
    """
    Base de todas las vistas de la web de administración (login de administradores, paso 3).

    - Exige el token de sesión (Authorization: Bearer <token>).
    - Exige que la cuenta esté ACTIVA.
    - Si la vista define `roles_permitidos`, exige uno de esos roles; si no, responde 403.
    - Con la contraseña temporal sin cambiar responde 403 CAMBIA_TU_CONTRASENA, salvo en las vistas
      con `permite_contrasena_temporal = True` (la sesión y cambiar la contraseña).
    """

    authentication_classes = [AdminAuthentication]
    permission_classes = [IsAdminActivo, SinContrasenaTemporal, RolPermitido]
    roles_permitidos = ()
    permite_contrasena_temporal = False

    def permission_denied(self, request, message=None, code=None):
        # Sin token: mensaje en español en lugar del de DRF.
        if not request.successful_authenticator:
            raise NotAuthenticated("Inicia sesión para continuar.")
        super().permission_denied(request, message=message, code=code)
