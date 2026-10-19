from django.db.models import Count, Max, Q, Sum
from django.utils import timezone

from moderacion.models import PerfilNegocio

from .models import ConsumoMensual, ConsumoToken, LimiteTokens


def inicio_del_mes_actual():
    """Primer instante del mes actual en la hora de Lima (TIME_ZONE)."""
    return timezone.localtime().replace(day=1, hour=0, minute=0, second=0, microsecond=0)


def _limites(usuario_ids):
    """Los límites propios de esas empresas y el límite por defecto (o None si no hay)."""
    propios, por_defecto = {}, None
    for limite in LimiteTokens.objects.filter(Q(usuario_id__in=usuario_ids) | Q(usuario_id__isnull=True)):
        if limite.usuario_id is None:
            por_defecto = limite
        else:
            propios[limite.usuario_id] = limite
    return propios, por_defecto


def limite_de_empresa(usuario_id):
    """HU 19: el límite propio de la empresa si tiene uno; si no, el de por defecto; si no hay ninguno, None."""
    propios, por_defecto = _limites([usuario_id])
    return propios.get(usuario_id, por_defecto)


def _con_limite(fila, limite):
    """Agrega a la fila de una empresa su límite, el porcentaje consumido y si está en alerta (HU 19)."""
    if limite is None:
        fila.update(limite=None, porcentaje_aviso=None, porcentaje=None, alerta=False)
        return fila
    tokens = fila["tokens"]
    fila.update(
        limite=limite.limite_mensual,
        porcentaje_aviso=limite.porcentaje_aviso,
        porcentaje=round(tokens * 100 / limite.limite_mensual, 1),
        # Se compara con enteros para que el 80 % exacto no falle por redondeos.
        alerta=tokens * 100 >= limite.limite_mensual * limite.porcentaje_aviso,
    )
    return fila


def consumo_del_mes_actual():
    """HU 17, criterio 1: tokens que gastó cada empresa en el mes actual, de la que más gasta a la que menos.

    El nombre de la empresa sale de perfil_negocio; si no tiene perfil se muestra "Empresa #id".
    HU 19: cada empresa trae su límite y si pasó el porcentaje de aviso; "alertas" junta solo las que lo pasaron.
    """
    inicio = inicio_del_mes_actual()
    filas = list(
        ConsumoToken.objects.filter(fecha__gte=inicio)
        .values("usuario_id")
        .annotate(tokens=Sum("tokens_total"), generaciones=Count("id"), ultima_generacion=Max("fecha"))
        .order_by("-tokens", "usuario_id")
    )
    usuario_ids = [fila["usuario_id"] for fila in filas]

    nombres = dict(
        PerfilNegocio.objects.filter(usuario_id__in=usuario_ids).values_list("usuario_id", "nombre_comercial")
    )
    propios, por_defecto = _limites(usuario_ids)
    for fila in filas:
        fila["empresa"] = nombres.get(fila["usuario_id"], f"Empresa #{fila['usuario_id']}")
        _con_limite(fila, propios.get(fila["usuario_id"], por_defecto))

    alertas = sorted((fila for fila in filas if fila["alerta"]), key=lambda fila: -fila["porcentaje"])

    return {
        "mes": inicio.strftime("%Y-%m"),
        "total": sum(fila["tokens"] for fila in filas),
        "empresas": filas,
        "alertas": alertas,
    }


MAXIMO_MESES = 24


def meses_entre(desde, hasta):
    """HU 20: cuántos meses abarca el rango, contando los dos extremos."""
    return (hasta.year - desde.year) * 12 + (hasta.month - desde.month) + 1


def _meses_del_rango(desde, hasta):
    """El primer día de cada mes desde `desde` hasta `hasta`, los dos incluidos."""
    meses, mes = [], desde.replace(day=1)
    while mes <= hasta:
        meses.append(mes)
        mes = mes.replace(year=mes.year + 1, month=1) if mes.month == 12 else mes.replace(month=mes.month + 1)
    return meses


def historial_por_periodo(desde, hasta, usuario_id=None):
    """HU 20, criterio 1: tokens y generaciones de cada mes del periodo (también los meses en cero) y el total.

    Con usuario_id solo cuenta esa empresa.
    """
    consulta = ConsumoMensual.objects.filter(mes__gte=desde, mes__lte=hasta)
    if usuario_id is not None:
        consulta = consulta.filter(usuario_id=usuario_id)
    por_mes = {
        fila["mes"]: fila
        for fila in consulta.values("mes").annotate(suma_tokens=Sum("tokens"), suma_generaciones=Sum("generaciones"))
    }

    meses = []
    for mes in _meses_del_rango(desde, hasta):
        fila = por_mes.get(mes, {})
        meses.append({
            "mes": mes.strftime("%Y-%m"),
            "tokens": fila.get("suma_tokens") or 0,
            "generaciones": fila.get("suma_generaciones") or 0,
        })

    return {
        "desde": desde.strftime("%Y-%m"),
        "hasta": hasta.strftime("%Y-%m"),
        "total": sum(mes["tokens"] for mes in meses),
        "meses": meses,
    }
