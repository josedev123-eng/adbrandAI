from rest_framework.permissions import BasePermission


class IsAdminActivo(BasePermission):
    """
    Permiso que verifica que el usuario autenticado es un administrador ACTIVO.
    """

    def has_permission(self, request, view):
        # Si no hay autenticación (None), denegar
        if not request.user or not hasattr(request.user, 'estado'):
            return False
        return request.user.estado == request.user.ACTIVO

class RolPermitido(BasePermission):
    """
    HU 02, criterio 2: cada rol solo usa sus módulos.

    La vista dice qué roles la pueden usar en `roles_permitidos`. Si la lista está vacía,
    basta con haber iniciado sesión (cualquier rol).
    """

    message = "Tu rol no tiene acceso a esta sección."

    def has_permission(self, request, view):
        roles = getattr(view, "roles_permitidos", ())
        if not roles:
            return True
        return getattr(request.user.rol, "nombre", None) in roles
