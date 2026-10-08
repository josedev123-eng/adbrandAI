from django.shortcuts import get_object_or_404
from django.utils import timezone

from .models import Contenido


def contenidos_dudosos():
    """HU 14, criterio 1: solo los marcados como dudosos, el más antiguo primero (es una cola)."""
    return Contenido.objects.filter(estado=Contenido.DUDOSO).order_by("fecha_creacion", "id")


def obtener_contenido(contenido_id):
    """Devuelve 404 si el contenido no existe."""
    return get_object_or_404(Contenido, pk=contenido_id)


def moderar_contenido(contenido_id, accion, moderador_id, motivo_rechazo=None):
    """HU 15: aprueba o rechaza un contenido, registra el moderador y guarda el motivo si lo trae."""
    contenido = get_object_or_404(Contenido, pk=contenido_id)

    if contenido.estado != Contenido.DUDOSO:
        raise ValueError("Solo se pueden moderar contenidos en estado DUDOSO")

    if accion == "APROBAR":
        contenido.estado = Contenido.APROBADO
    elif accion == "RECHAZAR":
        contenido.estado = Contenido.RECHAZADO
    else:
        raise ValueError("Acción inválida. Use APROBAR o RECHAZAR")

    campos = ["estado", "moderador_id", "fecha_actualizacion"]
    contenido.moderador_id = moderador_id
    contenido.fecha_actualizacion = timezone.now()

    if accion == "RECHAZAR" and motivo_rechazo:
        contenido.motivo_revision = motivo_rechazo
        campos.append("motivo_revision")

    contenido.save(update_fields=campos)

    return contenido