from rest_framework.authentication import BaseAuthentication
from rest_framework.exceptions import AuthenticationFailed

from .models import UsuarioAdmin
from .sesion import leer_token


class AdminAuthentication(BaseAuthentication):
    """
    Autenticación de administradores (Sprint 2).
    Lee el token de "Authorization: Bearer <token>" que entrega POST /api/usuarios/login/.
    Por ahora también acepta el header X-Admin-Id mientras la web pasa a usar el token.
    En los dos casos validamos que el admin existe y está ACTIVO.
    """

    def authenticate(self, request):
        cabecera = request.headers.get("Authorization", "")
        if cabecera.startswith("Bearer "):
            admin_id = leer_token(cabecera[len("Bearer "):].strip())
            if admin_id is None:
                raise AuthenticationFailed("Tu sesión no es válida o ya venció. Inicia sesión de nuevo.")
        else:
            # Temporal: se quita cuando la web use solo el token.
            admin_id = request.headers.get("X-Admin-Id")
            if not admin_id:
                return None

        try:
            admin = UsuarioAdmin.objects.select_related("rol").get(pk=admin_id)
        except (UsuarioAdmin.DoesNotExist, ValueError):
            raise AuthenticationFailed("Administrador no encontrado.")

        if admin.estado != UsuarioAdmin.ACTIVO:
            raise AuthenticationFailed("Cuenta desactivada. Contacte al superadministrador.")

        return (admin, None)
