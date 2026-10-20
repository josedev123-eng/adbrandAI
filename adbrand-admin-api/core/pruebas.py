"""Ayudas para las pruebas: crear administradores e iniciar sesión con su token."""

from usuarios.models import Rol, UsuarioAdmin
from usuarios.sesion import crear_token


def crear_admin(nombre_rol, correo=None):
    """Crea un administrador ACTIVO con el rol pedido (crea el rol si no existe)."""
    rol, _ = Rol.objects.get_or_create(nombre=nombre_rol)
    return UsuarioAdmin.objects.create(
        nombres="Prueba", apellidos=nombre_rol.capitalize(),
        correo=correo or f"{nombre_rol.lower()}@prueba.pe",
        contrasena="sin-uso", rol=rol, estado=UsuarioAdmin.ACTIVO,
    )


def con_sesion(admin):
    """Headers para un pedido hecho por `admin`, como lo manda la web después del login."""
    return {"HTTP_AUTHORIZATION": f"Bearer {crear_token(admin)}"}


def iniciar_sesion(client, admin):
    """Desde aquí, todos los pedidos de `client` van con el token de `admin`."""
    client.credentials(**con_sesion(admin))
