import json
import logging
import time
import urllib.error
import urllib.request

from django.conf import settings
from django.db import DatabaseError, connection
from django.utils import timezone

from .models import VerificacionServidor

log = logging.getLogger(__name__)

NOMBRES = {
    VerificacionServidor.BACKEND: "Backend",
    VerificacionServidor.BASE_DATOS: "Base de datos",
    VerificacionServidor.IA: "Servicio de IA",
}
ESPERA_BACKEND = 3  # segundos
IA_SIN_BACKEND = "No se pudo verificar porque el backend no responde"


def _ms_desde(inicio):
    return round((time.monotonic() - inicio) * 1000)


def _resultado(servicio, estado, tiempo_ms=None, detalle=None):
    return {"servicio": servicio, "estado": estado, "tiempo_ms": tiempo_ms, "detalle": detalle}


def verificar_base_datos():
    """HU 18: la base de datos está activa si responde a un SELECT 1."""
    inicio = time.monotonic()
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
            cursor.fetchone()
    except DatabaseError:
        log.warning("La base de datos no respondió a la verificación", exc_info=True)
        return _resultado(VerificacionServidor.BASE_DATOS, VerificacionServidor.CAIDO,
                          detalle="La base de datos no responde")
    return _resultado(VerificacionServidor.BASE_DATOS, VerificacionServidor.ACTIVO, _ms_desde(inicio))


def pedir_estado_a_spring():
    """Llama a GET /api/estado de Spring Boot. Lanza OSError o ValueError si no responde bien."""
    url = f"{settings.CORE_API_URL.rstrip('/')}/api/estado"
    with urllib.request.urlopen(url, timeout=ESPERA_BACKEND) as respuesta:
        return json.load(respuesta)


def verificar_backend_e_ia():
    """HU 18: el backend está activo si Spring responde; el estado de la IA lo da Spring.

    Si Spring no responde, la IA no se puede verificar y también queda caída (criterio 2).
    """
    inicio = time.monotonic()
    try:
        datos = pedir_estado_a_spring()
    except (OSError, ValueError) as error:  # URLError, timeout, conexión rechazada o JSON roto
        log.warning("Spring Boot no respondió a la verificación: %s", error)
        return [
            _resultado(VerificacionServidor.BACKEND, VerificacionServidor.CAIDO, detalle="El backend no responde"),
            _resultado(VerificacionServidor.IA, VerificacionServidor.CAIDO, detalle=IA_SIN_BACKEND),
        ]

    backend = _resultado(VerificacionServidor.BACKEND, VerificacionServidor.ACTIVO, _ms_desde(inicio))
    ia = datos.get("ia") or {}
    estado_ia = ia.get("estado") if ia.get("estado") == VerificacionServidor.ACTIVO else VerificacionServidor.CAIDO
    detalle_ia = ia.get("detalle") or (None if estado_ia == VerificacionServidor.ACTIVO else "La IA no responde")
    return [backend, _resultado(VerificacionServidor.IA, estado_ia, ia.get("tiempo_ms"), detalle_ia)]


def _guardar(resultados, fecha):
    """Guarda cada verificación. Si la base de datos está caída no se puede guardar, pero la respuesta sigue."""
    try:
        VerificacionServidor.objects.bulk_create(
            VerificacionServidor(fecha=fecha, **resultado) for resultado in resultados
        )
    except DatabaseError:
        log.warning("No se pudo guardar la verificación del servidor", exc_info=True)


def verificar_estado():
    """HU 18, criterio 1: estado de cada servicio en este momento y la hora de la verificación."""
    backend, ia = verificar_backend_e_ia()
    resultados = [backend, verificar_base_datos(), ia]
    ahora = timezone.now()
    _guardar(resultados, ahora)

    return {
        "verificado_en": timezone.localtime(ahora).isoformat(),
        "servicios": [{**resultado, "nombre": NOMBRES[resultado["servicio"]]} for resultado in resultados],
    }
