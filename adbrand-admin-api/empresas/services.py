import logging

from django.conf import settings
from django.core.mail import send_mail
from django.db.models import Count, Q, Sum
from django.shortcuts import get_object_or_404
from django.utils import timezone

from auditoria.services import registrar_accion
from moderacion.models import Contenido
from suscripciones.models import Suscripcion
from tokens.models import ConsumoToken
from tokens.services import inicio_del_mes_actual

from .models import Empresa

log = logging.getLogger(__name__)


def listar_empresas(busqueda=None, estado=None):
    """Las PYMEs con lo que ayuda a detectar abusos: su plan, los tokens de IA del mes y cuántos
    anuncios les rechazaron. Las suspendidas primero, luego las que más rechazos tienen."""
    empresas = Empresa.objects.all()
    if busqueda:
        empresas = empresas.filter(Q(nombre_empresa__icontains=busqueda) | Q(correo__icontains=busqueda))
    if estado in (Empresa.ACTIVO, Empresa.INACTIVO):
        empresas = empresas.filter(estado=estado)
    empresas = list(empresas)
    ids = [empresa.id for empresa in empresas]

    tokens = dict(
        ConsumoToken.objects.filter(usuario_id__in=ids, fecha__gte=inicio_del_mes_actual())
        .values("usuario_id").annotate(total=Sum("tokens_total")).values_list("usuario_id", "total")
    )
    anuncios = {
        fila["usuario_id"]: fila
        for fila in Contenido.objects.filter(usuario_id__in=ids)
        .values("usuario_id")
        .annotate(total=Count("id"), rechazados=Count("id", filter=Q(estado=Contenido.RECHAZADO)))
    }
    hoy = timezone.localdate()
    planes = {}
    for suscripcion in Suscripcion.objects.filter(
        usuario_id__in=ids, estado=Suscripcion.ACTIVA, fecha_vencimiento__gte=hoy
    ).order_by("usuario_id", "-fecha_vencimiento", "-id"):
        planes.setdefault(suscripcion.usuario_id, suscripcion.plan)

    for empresa in empresas:
        empresa.plan = planes.get(empresa.id)
        empresa.tokens_mes = tokens.get(empresa.id, 0)
        empresa.anuncios = anuncios.get(empresa.id, {}).get("total", 0)
        empresa.rechazados = anuncios.get(empresa.id, {}).get("rechazados", 0)

    empresas.sort(key=lambda e: (e.estado != Empresa.INACTIVO, -e.rechazados, e.nombre_empresa.lower()))
    return empresas


def suspender(empresa_id, motivo, admin_id):
    """Suspende la cuenta: Spring bloquea su login y corta la sesión abierta. Queda en la auditoría
    y se le avisa por correo con el motivo."""
    empresa = get_object_or_404(Empresa, pk=empresa_id)
    if empresa.estado == Empresa.INACTIVO:
        raise ValueError("La empresa ya está suspendida.")
    empresa.estado = Empresa.INACTIVO
    empresa.motivo_suspension = motivo.strip()
    empresa.fecha_suspension = timezone.now()
    empresa.save(update_fields=["estado", "motivo_suspension", "fecha_suspension"])

    registrar_accion(
        admin_id, "SUSPENDER_EMPRESA", "usuario", empresa.id,
        f"Suspendió a {empresa.nombre_empresa} ({empresa.correo}). Motivo: {empresa.motivo_suspension}",
    )
    _avisar(
        empresa,
        "Tu cuenta de AdBrand.AI fue suspendida",
        [
            f"Hola, {empresa.nombre_empresa}:",
            "",
            "Suspendimos tu cuenta de AdBrand.AI.",
            f"Motivo: {empresa.motivo_suspension}",
            "",
            "Si crees que es un error, responde este correo y lo revisamos.",
        ],
    )
    return empresa


def reactivar(empresa_id, admin_id):
    """Quita la suspensión: la empresa puede volver a iniciar sesión."""
    empresa = get_object_or_404(Empresa, pk=empresa_id)
    if empresa.estado == Empresa.ACTIVO:
        raise ValueError("La empresa ya está activa.")
    empresa.estado = Empresa.ACTIVO
    empresa.motivo_suspension = None
    empresa.fecha_suspension = None
    empresa.save(update_fields=["estado", "motivo_suspension", "fecha_suspension"])

    registrar_accion(
        admin_id, "REACTIVAR_EMPRESA", "usuario", empresa.id,
        f"Reactivó a {empresa.nombre_empresa} ({empresa.correo})",
    )
    _avisar(
        empresa,
        "Tu cuenta de AdBrand.AI fue reactivada",
        [f"Hola, {empresa.nombre_empresa}:", "", "Tu cuenta de AdBrand.AI ya está activa. Puedes volver a iniciar sesión."],
    )
    return empresa


def _avisar(empresa, asunto, lineas):
    """Correo a la empresa. Si falla, la suspensión o la reactivación igual quedan hechas."""
    if not empresa.correo:
        return
    try:
        cuerpo = "\n".join(lineas + ["", "El equipo de AdBrand.AI"])
        send_mail(asunto, cuerpo, settings.DEFAULT_FROM_EMAIL, [empresa.correo])
    except Exception as error:  # un correo caído no debe deshacer la acción
        log.warning("No se pudo enviar el aviso a %s: %s", empresa.correo, error)
