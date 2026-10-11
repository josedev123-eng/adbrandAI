import logging

from django.db import transaction

from usuarios.models import UsuarioAdmin

from .models import AuditoriaAdmin

logger = logging.getLogger(__name__)


def registrar_accion(admin_id, accion, entidad, entidad_id=None, detalle=None):
    """HU 04: guarda qué hizo un administrador y sobre qué.

    Nunca lanza errores: si el registro falla, la acción original sigue su curso.
    Devuelve el registro creado, o None si no se pudo guardar.
    """
    try:
        # La moderación (HU 15) recibe el moderador escrito a mano: puede no existir.
        if not UsuarioAdmin.objects.filter(pk=admin_id).exists():
            return None

        with transaction.atomic():
            return AuditoriaAdmin.objects.create(
                admin_id=admin_id,
                accion=accion,
                entidad=entidad,
                entidad_id=entidad_id,
                detalle=detalle,
            )
    except Exception:
        logger.warning("No se pudo registrar la acción %s del administrador %s", accion, admin_id, exc_info=True)
        return None


def historial(admin_id=None, desde=None, hasta=None):
    """HU 04: las acciones registradas, de la más reciente a la más antigua.

    Los tres filtros son opcionales: por administrador y por rango de fechas (ambos días incluidos).
    """
    consulta = AuditoriaAdmin.objects.select_related("admin")

    if admin_id is not None:
        consulta = consulta.filter(admin_id=admin_id)
    if desde is not None:
        consulta = consulta.filter(fecha__date__gte=desde)
    if hasta is not None:
        consulta = consulta.filter(fecha__date__lte=hasta)

    return consulta.order_by("-fecha", "-id")
