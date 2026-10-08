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