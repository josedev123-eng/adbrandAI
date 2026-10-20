from rest_framework.exceptions import NotAuthenticated
from rest_framework.views import APIView

from .authentication import AdminAuthentication
from .permissions import IsAdminActivo, RolPermitido


class VistaAdmin(APIView):
    """
    Base de todas las vistas de la web de administración (login de administradores, paso 3).

    - Exige el token de sesión (Authorization: Bearer <token>).
    - Exige que la cuenta esté ACTIVA.
    - Si la vista define `roles_permitidos`, exige uno de esos roles; si no, responde 403.
    """

    authentication_classes = [AdminAuthentication]
    permission_classes = [IsAdminActivo, RolPermitido]
    roles_permitidos = ()

    def permission_denied(self, request, message=None, code=None):
        # Sin token: mensaje en español en lugar del de DRF.
        if not request.successful_authenticator:
            raise NotAuthenticated("Inicia sesión para continuar.")
        super().permission_denied(request, message=message, code=code)
