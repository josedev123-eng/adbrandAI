from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

from .models import UsuarioAdmin
from .sesion import leer_token


class AdminAuthentication(BaseAuthentication):
    """
    Identifica al administrador que hace el pedido con el token del login:
    la web manda "Authorization: Bearer <token>". Sin token devuelve None (no hay sesión).
    """

    def authenticate(self, request):
        cabecera = request.headers.get("Authorization", "")
        if not cabecera.startswith("Bearer "):
            return None

        admin_id = leer_token(cabecera.removeprefix("Bearer ").strip())
        if admin_id is None:
            raise AuthenticationFailed("Tu sesión venció o no es válida. Vuelve a iniciar sesión.")

        try:
            admin = UsuarioAdmin.objects.select_related("rol").get(pk=admin_id)
        except (UsuarioAdmin.DoesNotExist, ValueError):
            raise AuthenticationFailed("Administrador no encontrado.")

        # HU 03, criterio 1: una cuenta desactivada ya no puede entrar.
        if admin.estado != UsuarioAdmin.ACTIVO:
            raise AuthenticationFailed("Cuenta desactivada. Contacte al superadministrador.")

        return (admin, None)
