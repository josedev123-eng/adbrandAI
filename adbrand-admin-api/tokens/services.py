from django.db.models import Count, Max, Q, Sum
from django.utils import timezone

from moderacion.models import PerfilNegocio
from suscripciones.models import Plan, Suscripcion

from django.conf import settings

from .models import ConsumoImagen, ConsumoMensual, ConsumoToken, LimiteTokens


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


def _limites_de_planes(usuario_ids, por_defecto):
    """El límite del plan pagado activo de cada empresa (la suscripción que vence más tarde).

    Se devuelve como un LimiteTokens sin guardar, con el porcentaje de aviso del límite por defecto.
    """
    hoy = timezone.localdate()
    activas = (
        Suscripcion.objects.filter(
            usuario_id__in=usuario_ids, estado=Suscripcion.ACTIVA, fecha_vencimiento__gte=hoy
        )
        .order_by("usuario_id", "-fecha_vencimiento", "-id")
    )
    plan_por_empresa = {}
    for suscripcion in activas:
        plan_por_empresa.setdefault(suscripcion.usuario_id, suscripcion.plan)
    limites_por_plan = dict(
        Plan.objects.filter(nombre__in=set(plan_por_empresa.values())).values_list("nombre", "limite_tokens")
    )
    aviso = por_defecto.porcentaje_aviso if por_defecto else 80
    return {
        usuario_id: LimiteTokens(usuario_id=usuario_id, limite_mensual=limites_por_plan[plan], porcentaje_aviso=aviso)
        for usuario_id, plan in plan_por_empresa.items()
        if plan in limites_por_plan
    }


def _limite_que_aplica(usuario_id, propios, planes, por_defecto):
    """La misma regla que hace cumplir Spring (LimiteConsumoIa):
    1. el límite propio que puso el administrador; 2. el del plan pagado activo; 3. el por defecto (plan gratuito).
    """
    return propios.get(usuario_id) or planes.get(usuario_id) or por_defecto


def limite_de_empresa(usuario_id):
    """HU 19: el límite que aplica a la empresa (propio, de su plan o el por defecto); None si no hay ninguno."""
    propios, por_defecto = _limites([usuario_id])
    planes = _limites_de_planes([usuario_id], por_defecto)
    return _limite_que_aplica(usuario_id, propios, planes, por_defecto)


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
    planes = _limites_de_planes(usuario_ids, por_defecto)
    for fila in filas:
        fila["empresa"] = nombres.get(fila["usuario_id"], f"Empresa #{fila['usuario_id']}")
        _con_limite(fila, _limite_que_aplica(fila["usuario_id"], propios, planes, por_defecto))

    alertas = sorted((fila for fila in filas if fila["alerta"]), key=lambda fila: -fila["porcentaje"])

    return {
        "mes": inicio.strftime("%Y-%m"),
        "total": sum(fila["tokens"] for fila in filas),
        "empresas": filas,
        "alertas": alertas,
        "imagenes": imagenes_del_mes(inicio),
    }


def imagenes_del_mes(inicio=None):
    """Control de gasto de imágenes en el mes actual: borradores (gratis) y retoques (de pago) por empresa.

    Solo cuenta las imágenes reales (las de prueba no gastan). "limite_retoques" es el tope de retoques de pago
    por empresa al mes (el mismo que usa Spring); "en_limite" marca a las empresas que ya llegaron.
    """
    inicio = inicio or inicio_del_mes_actual()
    limite = settings.LIMITE_RETOQUES_MES
    filas = list(
        ConsumoImagen.objects.filter(fecha__gte=inicio, simulado=False)
        .values("usuario_id")
        .annotate(
            borradores=Count("id", filter=Q(tipo=ConsumoImagen.BORRADOR)),
            retoques=Count("id", filter=Q(tipo=ConsumoImagen.RETOQUE)),
            ultima=Max("fecha"),
        )
        .order_by("-retoques", "-borradores", "usuario_id")
    )
    nombres = dict(
        PerfilNegocio.objects.filter(usuario_id__in=[fila["usuario_id"] for fila in filas])
        .values_list("usuario_id", "nombre_comercial")
    )
    for fila in filas:
        fila["empresa"] = nombres.get(fila["usuario_id"], f"Empresa #{fila['usuario_id']}")
        fila["en_limite"] = fila["retoques"] >= limite
    return {
        "borradores": sum(fila["borradores"] for fila in filas),
        "retoques": sum(fila["retoques"] for fila in filas),
        "limite_retoques": limite,
        "empresas": filas,
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
