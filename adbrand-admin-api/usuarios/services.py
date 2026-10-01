from django.contrib.auth.hashers import make_password

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