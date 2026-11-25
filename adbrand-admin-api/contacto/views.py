from django.shortcuts import get_object_or_404
from rest_framework.response import Response

from usuarios.vistas import VistaAdmin

from .models import MensajeContacto
from .serializers import AtendidoSerializer, MensajeContactoSerializer

# Soporte: el superadministrador y el moderador responden los mensajes.
ROLES_CONTACTO = ("SUPERADMIN", "MODERADOR")


class MensajesContactoView(VistaAdmin):
    """GET /api/contacto/?pendientes=1 : los mensajes del formulario de Contacto (primero los no atendidos)."""

    roles_permitidos = ROLES_CONTACTO

    def get(self, request):
        mensajes = MensajeContacto.objects.all()
        if request.query_params.get("pendientes") in ("1", "true"):
            mensajes = mensajes.filter(atendido=False)
        return Response(MensajeContactoSerializer(mensajes, many=True).data)


class AtenderMensajeView(VistaAdmin):
    """PATCH /api/contacto/<id>/ {atendido} : marca el mensaje como atendido (o lo vuelve a pendiente)."""

    roles_permitidos = ROLES_CONTACTO

    def patch(self, request, mensaje_id):
        datos = AtendidoSerializer(data=request.data)
        datos.is_valid(raise_exception=True)
        mensaje = get_object_or_404(MensajeContacto, pk=mensaje_id)
        mensaje.atendido = datos.validated_data["atendido"]
        mensaje.save(update_fields=["atendido"])
        return Response(MensajeContactoSerializer(mensaje).data)
