from rest_framework.test import APITestCase

from core.pruebas import con_sesion
from usuarios.models import Rol, UsuarioAdmin

from .models import MensajeContacto

URL = "/api/contacto/"


class MensajesContactoTests(APITestCase):
    """Formulario de Contacto: el equipo ve los mensajes y los marca como atendidos."""

    @classmethod
    def setUpTestData(cls):
        cls.moderador = UsuarioAdmin.objects.create(
            nombres="Ana", apellidos="Soporte", correo="ana@adbrand.pe", contrasena="password123",
            rol=Rol.objects.create(nombre="MODERADOR"), estado=UsuarioAdmin.ACTIVO,
        )
        cls.finanzas = UsuarioAdmin.objects.create(
            nombres="Luis", apellidos="Caja", correo="luis@adbrand.pe", contrasena="password123",
            rol=Rol.objects.create(nombre="FINANZAS"), estado=UsuarioAdmin.ACTIVO,
        )
        cls.pendiente = MensajeContacto.objects.create(
            nombre="Rosa", correo="rosa@panaderia.pe", asunto="PROBLEMA", mensaje="No baja mi video.",
        )
        cls.atendido = MensajeContacto.objects.create(
            nombre="Luis", correo="luis@bodega.pe", asunto="DUDA", mensaje="¿Cómo cambio mi plan?", atendido=True,
        )

    def test_lista_primero_los_pendientes_con_el_nombre_del_motivo(self):
        respuesta = self.client.get(URL, **con_sesion(self.moderador))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual([m["id"] for m in respuesta.data], [self.pendiente.id, self.atendido.id])
        self.assertEqual(respuesta.data[0]["motivo"], "Algo no funciona")

    def test_filtra_solo_los_pendientes(self):
        respuesta = self.client.get(URL + "?pendientes=1", **con_sesion(self.moderador))

        self.assertEqual([m["id"] for m in respuesta.data], [self.pendiente.id])

    def test_marca_un_mensaje_como_atendido(self):
        respuesta = self.client.patch(
            f"{URL}{self.pendiente.id}/", {"atendido": True}, format="json", **con_sesion(self.moderador)
        )

        self.assertEqual(respuesta.status_code, 200)
        self.pendiente.refresh_from_db()
        self.assertTrue(self.pendiente.atendido)

    def test_finanzas_no_ve_los_mensajes(self):
        respuesta = self.client.get(URL, **con_sesion(self.finanzas))

        self.assertEqual(respuesta.status_code, 403)
