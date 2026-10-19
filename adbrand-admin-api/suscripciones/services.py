from datetime import date
from decimal import Decimal

from django.db.models import Q, Sum
from django.db.models.functions import TruncMonth
from django.utils import timezone

from .models import Pago, Suscripcion

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


def total_suscripciones_activas():
    """HU 05: cuántas suscripciones están en uso hoy.

    Activa = estado ACTIVA con fecha_vencimiento de hoy en adelante. Una ACTIVA cuya
    fecha ya pasó cuenta como vencida (la misma regla de la HU 08), así que no suma.
    """
    hoy = timezone.localdate()

    return Suscripcion.objects.filter(estado=Suscripcion.ACTIVA, fecha_vencimiento__gte=hoy).count()


MESES_POR_DEFECTO = 6
MAXIMO_MESES = 24


def _mes_desplazado(mes, cantidad):
    """Primer día del mes que está `cantidad` meses después (o antes, si es negativa)."""
    total = mes.year * 12 + (mes.month - 1) + cantidad
    return date(total // 12, total % 12 + 1, 1)


def meses_entre(desde, hasta):
    """HU 07: cuántos meses abarca el rango, contando los dos extremos."""
    return (hasta.year - desde.year) * 12 + (hasta.month - desde.month) + 1


def ingresos_mensuales(desde=None, hasta=None):
    """HU 06: lo cobrado en cada uno de los últimos 6 meses, contando el actual.

    El ingreso es el pago recibido (tabla pago), no el monto de la suscripción.
    Devuelve una fila por mes, del más antiguo al más reciente; un mes sin pagos sale con 0.00.

    HU 07: con `desde` y `hasta` (el primer día de cada mes) devuelve ese rango,
    los dos meses incluidos, en lugar de los últimos 6.
    """
    if desde is not None and hasta is not None:
        primer_mes = desde.replace(day=1)
        mes_siguiente = _mes_desplazado(hasta.replace(day=1), 1)
    else:
        mes_actual = timezone.localdate().replace(day=1)
        primer_mes = _mes_desplazado(mes_actual, -(MESES_POR_DEFECTO - 1))
        mes_siguiente = _mes_desplazado(mes_actual, 1)

    cobrado = (
        Pago.objects.filter(fecha_pago__gte=primer_mes, fecha_pago__lt=mes_siguiente)
        .annotate(mes=TruncMonth("fecha_pago"))
        .values("mes")
        .annotate(total=Sum("monto"))
    )
    total_por_mes = {fila["mes"]: fila["total"] for fila in cobrado}

    ingresos = []
    mes = primer_mes
    while mes < mes_siguiente:
        ingresos.append({"mes": mes.strftime("%Y-%m"), "total": total_por_mes.get(mes, Decimal("0.00"))})
        mes = _mes_desplazado(mes, 1)

    return ingresos
