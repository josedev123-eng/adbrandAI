from django.contrib.auth.hashers import make_password
from django.shortcuts import get_object_or_404

from .models import UsuarioAdmin


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