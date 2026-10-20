"""Token de sesión de los administradores.

Usamos la firma de Django (django.core.signing): el token lleva el id del administrador
firmado con la SECRET_KEY, así nadie puede cambiarlo sin que se note. No hace falta
instalar ninguna librería nueva.
"""
from django.core import signing

SAL = "adbrand-admin-sesion"
DURACION_SEGUNDOS = 8 * 60 * 60  # una jornada de trabajo: 8 horas


def crear_token(admin):
    """Devuelve el token que la web guarda y manda en cada pedido."""
    return signing.dumps({"admin_id": admin.id}, salt=SAL)


def leer_token(token):
    """Devuelve el id del administrador, o None si el token es falso o ya venció."""
    try:
        datos = signing.loads(token, salt=SAL, max_age=DURACION_SEGUNDOS)
    except signing.BadSignature:  # también cubre SignatureExpired
        return None
    return datos.get("admin_id")