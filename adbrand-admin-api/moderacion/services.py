from django.shortcuts import get_object_or_404
from django.utils import timezone

from .models import Contenido


def contenidos_dudosos():
    """HU 14, criterio 1: solo los marcados como dudosos, el más antiguo primero (es una cola)."""
    return Contenido.objects.filter(estado=Contenido.DUDOSO).order_by("fecha_creacion", "id")


def obtener_contenido(contenido_id):
    """Devuelve 404 si el contenido no existe."""
    return get_object_or_404(Contenido, pk=contenido_id)


def moderar_contenido(contenido_id, accion, moderador_id):
    """HU 15: aprueba o rechaza un contenido y registra el moderador."""
    contenido = get_object_or_404(Contenido, pk=contenido_id)

    if contenido.estado != Contenido.DUDOSO:
        raise ValueError("Solo se pueden moderar contenidos en estado DUDOSO")

    if accion == "APROBAR":
        contenido.estado = Contenido.APROBADO
    elif accion == "RECHAZAR":
        contenido.estado = Contenido.RECHAZADO
    else:
        raise ValueError("Acción inválida. Use APROBAR o RECHAZAR")

    contenido.moderador_id = moderador_id
    contenido.fecha_actualizacion = timezone.now()
    contenido.save(update_fields=["estado", "moderador_id", "fecha_actualizacion"])

    return contenido


def actualizar_estado(contenido_id, estado, moderador_id, motivo_rechazo=None):
    """HU 15: actualiza el estado del contenido con moderador y motivo opcional."""
    contenido = get_object_or_404(Contenido, pk=contenido_id)

    if contenido.estado != Contenido.DUDOSO:
        raise ValueError("Solo se pueden moderar contenidos en estado DUDOSO")

    if estado not in [Contenido.APROBADO, Contenido.RECHAZADO]:
        raise ValueError("Estado inválido. Use APROBADO o RECHAZADO")

    if estado == Contenido.RECHAZADO and not motivo_rechazo:
        raise ValueError("El motivo de rechazo es obligatorio al rechazar contenido.")

    contenido.estado = estado
    contenido.moderador_id = moderador_id
    contenido.fecha_actualizacion = timezone.now()

    if motivo_rechazo:
        contenido.motivo_revision = motivo_rechazo

    contenido.save(update_fields=["estado", "moderador_id", "fecha_actualizacion", "motivo_revision"])

    return contenido