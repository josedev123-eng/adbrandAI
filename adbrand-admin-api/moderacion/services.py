from math import ceil

import logging

from django.conf import settings
from django.core.mail import send_mail
from django.shortcuts import get_object_or_404
from django.utils import timezone

from empresas.models import Empresa

from .models import Contenido, HistorialContenido, PerfilNegocio

log = logging.getLogger(__name__)


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

    # El dueño se entera: queda el aviso para "Mis anuncios" (V44) y se le envía un correo.
    campos = ["estado", "moderador_id", "fecha_actualizacion", "aviso_pendiente"]
    contenido.moderador_id = moderador_id
    contenido.fecha_actualizacion = timezone.now()
    contenido.aviso_pendiente = True

    if accion == "RECHAZAR" and motivo_rechazo:
        contenido.motivo_revision = motivo_rechazo
        campos.append("motivo_revision")

    contenido.save(update_fields=campos)
    avisar_al_dueno(contenido)

    return contenido


def avisar_al_dueno(contenido):
    """Correo al dueño con lo que decidió el moderador. Si el correo falla, la moderación igual queda hecha."""
    empresa = Empresa.objects.filter(pk=contenido.usuario_id).first()
    if not empresa or not empresa.correo:
        return
    if contenido.estado == Contenido.APROBADO:
        asunto = "Tu anuncio fue aprobado"
        lineas = [
            f"Hola, {empresa.nombre_empresa}:",
            "",
            f'Revisamos tu anuncio "{contenido.oferta}" y ya está aprobado.',
            "Puedes copiarlo y programarlo en tu calendario desde AdBrand.AI.",
        ]
    else:
        asunto = "Tu anuncio no fue aprobado"
        motivo = contenido.motivo_revision or "No cumple las normas de publicidad."
        lineas = [
            f"Hola, {empresa.nombre_empresa}:",
            "",
            f'Revisamos tu anuncio "{contenido.oferta}" y no pudimos aprobarlo.',
            f"Motivo: {motivo}",
            "",
            'Puedes generar una versión nueva desde "Crear anuncio" en AdBrand.AI.',
        ]
    cuerpo = "\n".join(lineas + ["", "El equipo de AdBrand.AI"])
    try:
        send_mail(asunto, cuerpo, settings.DEFAULT_FROM_EMAIL, [empresa.correo])
    except Exception as error:  # un correo caído no debe deshacer la moderación
        log.warning("No se pudo enviar el aviso de moderación a %s: %s", empresa.correo, error)

POR_PAGINA = 20


def historial(estado=None, desde=None, hasta=None, pagina=1):
    """HU 16: contenidos aprobados y rechazados, del más reciente al más antiguo, 20 por página.

    Criterio 2: se filtra por estado (APROBADO o RECHAZADO) y por la fecha de la decisión (los dos días incluidos).
    """
    consulta = HistorialContenido.objects.all()
    if estado:
        consulta = consulta.filter(estado=estado)
    if desde:
        consulta = consulta.filter(fecha_decision__date__gte=desde)
    if hasta:
        consulta = consulta.filter(fecha_decision__date__lte=hasta)
    consulta = consulta.order_by("-fecha_decision", "-id")

    total = consulta.count()
    paginas = max(1, ceil(total / POR_PAGINA))
    inicio = (pagina - 1) * POR_PAGINA
    filas = list(consulta[inicio:inicio + POR_PAGINA])

    nombres = dict(
        PerfilNegocio.objects.filter(usuario_id__in={fila.usuario_id for fila in filas})
        .values_list("usuario_id", "nombre_comercial")
    )
    for fila in filas:
        fila.empresa = nombres.get(fila.usuario_id, f"Empresa #{fila.usuario_id}")

    return {"total": total, "pagina": pagina, "paginas": paginas, "resultados": filas}
