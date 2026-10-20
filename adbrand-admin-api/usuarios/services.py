from django.contrib.auth.hashers import check_password, make_password
from django.shortcuts import get_object_or_404

from .models import UsuarioAdmin


class CuentaDesactivada(Exception):
    """La contraseña es correcta pero la cuenta está INACTIVA (HU 03)."""


def iniciar_sesion(correo, contrasena):
    """Sprint 2: devuelve el administrador si el correo y la contraseña son correctos, o None.

    Si la cuenta está desactivada lanza CuentaDesactivada.
    """
    admin = UsuarioAdmin.objects.select_related("rol").filter(correo__iexact=correo).first()
    if admin is None or not check_password(contrasena, admin.contrasena):
        return None

    if admin.estado != UsuarioAdmin.ACTIVO:
        raise CuentaDesactivada()

    return admin


def crear_administrador(datos):
    """Guarda un administrador nuevo con su contraseña cifrada."""
    return UsuarioAdmin.objects.create(
        nombres=datos["nombres"].strip(),
        apellidos=datos["apellidos"].strip(),
        correo=datos["correo"],
        contrasena=make_password(datos["contrasena"]),
        rol=datos["rol"],
    )


def desactivar_administrador(admin_id):
    """HU 03: desactiva un administrador cambiando su estado a INACTIVO."""
    admin = get_object_or_404(UsuarioAdmin, pk=admin_id)

    if admin.estado == UsuarioAdmin.INACTIVO:
        raise ValueError("El administrador ya está desactivado.")

    admin.estado = UsuarioAdmin.INACTIVO
    admin.save(update_fields=["estado"])

    return admin


def cambiar_rol(admin_id, rol):
    """HU 02: cambia el rol de un administrador; sus permisos dependen del rol."""
    admin = get_object_or_404(UsuarioAdmin, pk=admin_id)

    admin.rol = rol
    admin.save(update_fields=["rol"])

    return admin