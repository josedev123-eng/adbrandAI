from django.db.models import Q
from django.utils import timezone

from .models import Suscripcion

ESTADOS_CON_DEUDA = ("VENCIDA", "PENDIENTE_PAGO")
ESTADOS_ACEPTADOS = ("VENCIDA", "PENDIENTE_PAGO")


def _esta_vencida(hoy):
    """Una suscripción vencida o una ACTIVA cuya fecha de vencimiento ya pasó."""
    return Q(estado=Suscripcion.VENCIDA) | Q(estado=Suscripcion.ACTIVA, fecha_vencimiento__lt=hoy)


def suscripciones_con_deuda(estado=None):
    """HU 08: las que hay que cobrar o regularizar, de la más antigua a la más reciente.

    - VENCIDA: estado VENCIDA, o ACTIVA cuya fecha_vencimiento ya pasó (cuenta como vencida).
    - PENDIENTE_PAGO: estado PENDIENTE_PAGO.
    Sin filtro devuelve las dos cosas juntas.
    """
    hoy = timezone.localdate()

    if estado is None:
        consulta = Suscripcion.objects.filter(_esta_vencida(hoy) | Q(estado=Suscripcion.PENDIENTE_PAGO))
    elif estado == Suscripcion.VENCIDA:
        consulta = Suscripcion.objects.filter(_esta_vencida(hoy))
    elif estado == Suscripcion.PENDIENTE_PAGO:
        consulta = Suscripcion.objects.filter(estado=Suscripcion.PENDIENTE_PAGO)
    else:
        raise ValueError("Estado inválido. Use VENCIDA o PENDIENTE_PAGO")

    return consulta.order_by("fecha_vencimiento", "id")
