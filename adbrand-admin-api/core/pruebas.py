"""Ayudas para las pruebas: crear administradores, iniciar sesión con su token y usar vistas de Flyway."""

import re

from django.conf import settings
from django.db import connection

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


MIGRACIONES = settings.BASE_DIR.parent / "adbrand-core-api" / "src" / "main" / "resources" / "db" / "migration"


def usar_vista_de_flyway(archivo):
    """La base de prueba crea las vistas de Flyway como tablas comunes (desde el modelo).

    Esto cambia esa tabla por la vista real, con el mismo SQL de la migración, para probar la consulta de verdad.
    Usarlo dentro de setUpTestData: al terminar la clase de pruebas se deshace solo.
    """
    sql = (MIGRACIONES / archivo).read_text(encoding="utf-8")
    nombre = re.search(r"CREATE VIEW (\w+)", sql).group(1)
    with connection.cursor() as cursor:
        cursor.execute(f"DROP TABLE IF EXISTS {nombre}")
        cursor.execute(sql)
