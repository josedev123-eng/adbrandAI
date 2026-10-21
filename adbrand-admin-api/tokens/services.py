from django.db.models import Count, Max, Sum
from django.utils import timezone

from moderacion.models import PerfilNegocio

from .models import ConsumoToken


def inicio_del_mes_actual():
    """Primer instante del mes actual en la hora de Lima (TIME_ZONE)."""
    return timezone.localtime().replace(day=1, hour=0, minute=0, second=0, microsecond=0)


def consumo_del_mes_actual():
    """HU 17, criterio 1: tokens que gastó cada empresa en el mes actual, de la que más gasta a la que menos.

    El nombre de la empresa sale de perfil_negocio; si no tiene perfil se muestra "Empresa #id".
    """
    inicio = inicio_del_mes_actual()
    filas = list(
        ConsumoToken.objects.filter(fecha__gte=inicio)
        .values("usuario_id")
        .annotate(tokens=Sum("tokens_total"), generaciones=Count("id"), ultima_generacion=Max("fecha"))
        .order_by("-tokens", "usuario_id")
    )

    nombres = dict(
        PerfilNegocio.objects.filter(usuario_id__in=[fila["usuario_id"] for fila in filas])
        .values_list("usuario_id", "nombre_comercial")
    )
    for fila in filas:
        fila["empresa"] = nombres.get(fila["usuario_id"], f"Empresa #{fila['usuario_id']}")

    return {
        "mes": inicio.strftime("%Y-%m"),
        "total": sum(fila["tokens"] for fila in filas),
        "empresas": filas,
    }
