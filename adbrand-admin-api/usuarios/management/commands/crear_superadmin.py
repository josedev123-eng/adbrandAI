"""Crea el primer SUPERADMIN desde la consola (Sprint 2).

Con el login, solo un superadmin puede crear cuentas. Para el primero usamos este comando:
    python manage.py crear_superadmin --nombres Jose --apellidos Rojas --correo jose@adbrand.pe
La contraseña se pide aparte, sin mostrarla en pantalla, y sigue las mismas reglas de la HU 01.
"""
from getpass import getpass

from django.core.management.base import BaseCommand, CommandError

from usuarios import services
from usuarios.models import Rol
from usuarios.serializers import CrearAdministradorSerializer


class Command(BaseCommand):
    help = "Crea una cuenta de administrador con rol SUPERADMIN."

    def add_arguments(self, parser):
        parser.add_argument("--nombres", required=True)
        parser.add_argument("--apellidos", required=True)
        parser.add_argument("--correo", required=True)
        parser.add_argument("--contrasena", help="Solo para pruebas; si no se pasa, se pide por consola.")

    def handle(self, *args, **opciones):
        rol = Rol.objects.filter(nombre="SUPERADMIN").first()
        if rol is None:
            raise CommandError("No existe el rol SUPERADMIN. Arranca Spring una vez para que Flyway cree las tablas.")

        contrasena = opciones["contrasena"] or getpass("Contraseña: ")
        serializer = CrearAdministradorSerializer(data={
            "nombres": opciones["nombres"],
            "apellidos": opciones["apellidos"],
            "correo": opciones["correo"],
            "contrasena": contrasena,
            "rol_id": rol.id,
        })
        if not serializer.is_valid():
            errores = "; ".join(f"{campo}: {' '.join(map(str, msgs))}" for campo, msgs in serializer.errors.items())
            raise CommandError(errores)

        admin = services.crear_administrador(serializer.validated_data)
        self.stdout.write(self.style.SUCCESS(f"Superadmin creado: {admin.correo} (id {admin.id})"))