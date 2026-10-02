from django.shortcuts import get_object_or_404

from .models import Contenido


def contenidos_dudosos():
    """HU 14, criterio 1: solo los marcados como dudosos, el más antiguo primero (es una cola)."""
    return Contenido.objects.filter(estado=Contenido.DUDOSO).order_by("fecha_creacion", "id")


def obtener_contenido(contenido_id):
    """Devuelve 404 si el contenido no existe."""
    return get_object_or_404(Contenido, pk=contenido_id)