from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

from .models import UsuarioAdmin


class AdminAuthentication(BaseAuthentication):
    """
    Autenticación simple por header X-Admin-Id.
    En producción usar JWT/OAuth2. Aquí validamos que el admin existe y está ACTIVO.
    """

    def authenticate(self, request):
        admin_id = request.headers.get("X-Admin-Id")
        if not admin_id:
            return None

        try:
            admin = UsuarioAdmin.objects.get(pk=admin_id)
        except UsuarioAdmin.DoesNotExist:
            raise AuthenticationFailed("Administrador no encontrado.")

        if admin.estado != UsuarioAdmin.ACTIVO:
            raise AuthenticationFailed("Cuenta desactivada. Contacte al superadministrador.")

        return (admin, None)