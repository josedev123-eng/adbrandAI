import logging
import secrets
from datetime import timedelta

from django.conf import settings
from django.contrib.auth.hashers import check_password, make_password
from django.core.mail import send_mail
from django.shortcuts import get_object_or_404
from django.utils import timezone

from .models import CodigoRecuperacionAdmin, UsuarioAdmin

log = logging.getLogger(__name__)

MINUTOS_CODIGO = 15
INTENTOS_MAXIMOS = 5


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


class ContrasenaActualIncorrecta(Exception):
    """Mi cuenta: la contraseña actual que escribió no coincide."""


class CodigoInvalido(Exception):
    """Olvidé mi contraseña: el código no existe, venció, ya se usó o tuvo demasiados intentos."""


def cambiar_mi_contrasena(admin, actual, nueva):
    """El administrador conectado cambia su propia contraseña. Debe escribir la actual."""
    if not check_password(actual, admin.contrasena):
        raise ContrasenaActualIncorrecta()
    admin.contrasena = make_password(nueva)
    admin.save(update_fields=["contrasena"])
    return admin


def solicitar_codigo(correo):
    """Manda un código de 6 dígitos al correo si es de un administrador activo.

    Devuelve el administrador o None. La vista responde lo mismo en los dos casos
    para no revelar qué correos están registrados.
    """
    admin = UsuarioAdmin.objects.filter(correo__iexact=correo, estado=UsuarioAdmin.ACTIVO).first()
    if admin is None:
        return None

    # Un código nuevo anula los anteriores que no se usaron.
    CodigoRecuperacionAdmin.objects.filter(admin=admin, usado=False).update(usado=True)
    codigo = f"{secrets.randbelow(1_000_000):06d}"
    CodigoRecuperacionAdmin.objects.create(
        admin=admin,
        codigo_hash=make_password(codigo),
        expira_en=timezone.now() + timedelta(minutes=MINUTOS_CODIGO),
    )
    try:
        send_mail(
            "Tu código para cambiar la contraseña · Panel AdBrand.AI",
            "\n".join([
                f"Hola, {admin.nombres}:",
                "",
                f"Tu código para crear una contraseña nueva es: {codigo}",
                f"Vence en {MINUTOS_CODIGO} minutos y sirve una sola vez.",
                "",
                "Si no lo pediste, ignora este correo: tu contraseña sigue igual.",
                "",
                "El equipo de AdBrand.AI",
            ]),
            settings.DEFAULT_FROM_EMAIL,
            [admin.correo],
        )
    except Exception as error:  # sin correo no se puede seguir, pero no se revela el motivo al público
        log.warning("No se pudo enviar el código de recuperación a %s: %s", admin.correo, error)
    return admin


def cambiar_con_codigo(correo, codigo, nueva):
    """Cambia la contraseña si el código es válido. Lanza CodigoInvalido si no."""
    admin = UsuarioAdmin.objects.filter(correo__iexact=correo, estado=UsuarioAdmin.ACTIVO).first()
    if admin is None:
        raise CodigoInvalido()

    registro = (
        CodigoRecuperacionAdmin.objects.filter(admin=admin, usado=False, expira_en__gt=timezone.now())
        .order_by("-fecha_creacion")
        .first()
    )
    if registro is None or registro.intentos >= INTENTOS_MAXIMOS:
        raise CodigoInvalido()

    if not check_password(codigo, registro.codigo_hash):
        registro.intentos += 1
        registro.save(update_fields=["intentos"])
        raise CodigoInvalido()

    registro.usado = True
    registro.save(update_fields=["usado"])
    admin.contrasena = make_password(nueva)
    admin.save(update_fields=["contrasena"])
    return admin
