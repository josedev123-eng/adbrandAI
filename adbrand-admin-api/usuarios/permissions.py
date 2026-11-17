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

class SinContrasenaTemporal(BasePermission):
    """Con la contraseña temporal (V55) solo se puede ver la sesión y cambiar la contraseña.

    Las vistas que sí se permiten lo dicen con `permite_contrasena_temporal = True`.
    """

    message = {
        "detail": "Antes de continuar, cambia la contraseña temporal que te dio el superadministrador.",
        "codigo": "CAMBIA_TU_CONTRASENA",
    }

    def has_permission(self, request, view):
        if getattr(view, "permite_contrasena_temporal", False):
            return True
        return not getattr(request.user, "debe_cambiar_contrasena", False)


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
