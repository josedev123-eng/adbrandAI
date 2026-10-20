from datetime import timedelta
from unittest.mock import patch

from django.utils import timezone
from rest_framework.test import APITestCase

from core.pruebas import con_sesion, iniciar_sesion
from moderacion.models import Contenido
from usuarios.models import Rol, UsuarioAdmin

from . import services
from .models import AuditoriaAdmin

URL = "/api/auditoria/"


def crear_administradores(cls):
    cls.superadmin_rol = Rol.objects.create(nombre="SUPERADMIN")
    cls.moderador_rol = Rol.objects.create(nombre="MODERADOR")
    cls.finanzas_rol = Rol.objects.create(nombre="FINANZAS")
    cls.superadmin = UsuarioAdmin.objects.create(
        nombres="Super", apellidos="Admin", correo="super@adbrand.pe",
        contrasena="password123", rol=cls.superadmin_rol, estado=UsuarioAdmin.ACTIVO,
    )
    cls.moderador = UsuarioAdmin.objects.create(
        nombres="Ana", apellidos="Moderadora", correo="ana@adbrand.pe",
        contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO,
    )
    cls.objetivo = UsuarioAdmin.objects.create(
        nombres="Juan", apellidos="Perez", correo="juan@adbrand.pe",
        contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO,
    )


def crear_contenido_dudoso(oferta):
    ahora = timezone.now()
    return Contenido.objects.create(
        usuario_id=1, tipo="ANUNCIO", red_social="INSTAGRAM", tono="CERCANO",
        oferta=oferta, texto=f"¡Esto no te lo puedes perder! {oferta}.",
        estado="DUDOSO", motivo_revision='"cigarrillos": Publicidad de tabaco.',
        fecha_creacion=ahora, fecha_actualizacion=ahora,
    )


class RegistrarAccionesTests(APITestCase):
    """HU-04: cada acción crítica de un administrador queda registrada."""

    @classmethod
    def setUpTestData(cls):
        crear_administradores(cls)

    def _como(self, admin):
        return con_sesion(admin)

    def test_se_registra_al_desactivar_una_cuenta(self):
        url = f"/api/usuarios/administradores/{self.objetivo.id}/desactivar/"
        respuesta = self.client.patch(url, {"confirmar": True}, format="json", **self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 200)
        registro = AuditoriaAdmin.objects.get()
        self.assertEqual(registro.admin_id, self.superadmin.id)
        self.assertEqual(registro.accion, "DESACTIVAR_ADMIN")
        self.assertEqual(registro.entidad, "usuario_admin")
        self.assertEqual(registro.entidad_id, self.objetivo.id)
        self.assertIn("juan@adbrand.pe", registro.detalle)

    def test_se_registra_al_cambiar_el_rol(self):
        url = f"/api/usuarios/administradores/{self.objetivo.id}/rol/"
        respuesta = self.client.patch(
            url, {"rol_id": self.finanzas_rol.id}, format="json", **self._como(self.superadmin)
        )

        self.assertEqual(respuesta.status_code, 200)
        registro = AuditoriaAdmin.objects.get()
        self.assertEqual(registro.admin_id, self.superadmin.id)
        self.assertEqual(registro.accion, "CAMBIAR_ROL")
        self.assertEqual(registro.entidad_id, self.objetivo.id)
        self.assertIn("FINANZAS", registro.detalle)

    def test_se_registra_al_aprobar_un_contenido(self):
        contenido = crear_contenido_dudoso("Cigarrillos importados")
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{contenido.id}/moderar/",
            {"accion": "APROBAR"},
            format="json",
            **self._como(self.moderador),
        )

        self.assertEqual(respuesta.status_code, 200)
        registro = AuditoriaAdmin.objects.get()
        self.assertEqual(registro.admin_id, self.moderador.id)
        self.assertEqual(registro.accion, "APROBAR_CONTENIDO")
        self.assertEqual(registro.entidad, "contenido")
        self.assertEqual(registro.entidad_id, contenido.id)

    def test_se_registra_al_rechazar_un_contenido(self):
        contenido = crear_contenido_dudoso("Apuestas deportivas")
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{contenido.id}/moderar/",
            {"accion": "RECHAZAR", "motivo_rechazo": "Publicidad de apuestas"},
            format="json",
            **self._como(self.moderador),
        )

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(AuditoriaAdmin.objects.get().accion, "RECHAZAR_CONTENIDO")

    def test_una_accion_rechazada_no_se_registra(self):
        url = f"/api/usuarios/administradores/{self.objetivo.id}/rol/"
        respuesta = self.client.patch(
            url, {"rol_id": self.finanzas_rol.id}, format="json", **self._como(self.moderador)
        )

        self.assertEqual(respuesta.status_code, 403)
        self.assertEqual(AuditoriaAdmin.objects.count(), 0)

    def test_un_moderador_que_no_existe_no_puede_moderar(self):
        # Antes el moderador_id venía en el cuerpo; ahora sale del token. Si la cuenta
        # ya no existe, Django rechaza el pedido y no se modera ni se registra nada.
        contenido = crear_contenido_dudoso("Cigarrillos importados")
        cabeceras = self._como(self.moderador)
        UsuarioAdmin.objects.filter(pk=self.moderador.pk).delete()

        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{contenido.id}/moderar/",
            {"accion": "APROBAR"},
            format="json",
            **cabeceras,
        )

        self.assertEqual(respuesta.status_code, 403)
        contenido.refresh_from_db()
        self.assertEqual(contenido.estado, "DUDOSO")
        self.assertEqual(AuditoriaAdmin.objects.count(), 0)

    def test_si_falla_el_registro_la_accion_original_se_completa(self):
        url = f"/api/usuarios/administradores/{self.objetivo.id}/desactivar/"
        with patch.object(AuditoriaAdmin.objects, "create", side_effect=RuntimeError("sin tabla")):
            # El fallo se anota en el log como advertencia, no se propaga.
            with self.assertLogs("auditoria.services", level="WARNING"):
                respuesta = self.client.patch(url, {"confirmar": True}, format="json", **self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 200)
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.estado, UsuarioAdmin.INACTIVO)
        self.assertEqual(AuditoriaAdmin.objects.count(), 0)


class ConsultarAuditoriaTests(APITestCase):
    """HU-04: consulta del historial con filtros por administrador y por fechas."""

    @classmethod
    def setUpTestData(cls):
        crear_administradores(cls)

        # Al mediodía, para que restar días no cambie de fecha cerca de la medianoche.
        cls.hoy = timezone.localtime().replace(hour=12, minute=0, second=0, microsecond=0)

        cls.antiguo = services.registrar_accion(
            cls.superadmin.id, "DESACTIVAR_ADMIN", "usuario_admin", cls.objetivo.id, "Desactivó la cuenta de juan@adbrand.pe"
        )
        cls.intermedio = services.registrar_accion(
            cls.moderador.id, "APROBAR_CONTENIDO", "contenido", 7, "2 panetones por S/ 35"
        )
        cls.reciente = services.registrar_accion(
            cls.superadmin.id, "CAMBIAR_ROL", "usuario_admin", cls.objetivo.id, "Cambió el rol de juan@adbrand.pe a FINANZAS"
        )
        AuditoriaAdmin.objects.filter(pk=cls.antiguo.pk).update(fecha=cls.hoy - timedelta(days=10))
        AuditoriaAdmin.objects.filter(pk=cls.intermedio.pk).update(fecha=cls.hoy - timedelta(days=5))
        AuditoriaAdmin.objects.filter(pk=cls.reciente.pk).update(fecha=cls.hoy - timedelta(days=1))

    def setUp(self):
        iniciar_sesion(self.client, self.superadmin)

    def ids(self, respuesta):
        return [fila["id"] for fila in respuesta.data]

    def dia(self, dias_atras):
        return (self.hoy - timedelta(days=dias_atras)).date().isoformat()

    def test_sin_filtros_devuelve_todo_del_mas_reciente_al_mas_antiguo(self):
        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(self.ids(respuesta), [self.reciente.id, self.intermedio.id, self.antiguo.id])

    def test_cada_fila_trae_el_nombre_y_el_correo_del_administrador(self):
        fila = self.client.get(URL).data[0]

        self.assertEqual(fila["admin"], {"id": self.superadmin.id, "nombre": "Super Admin", "correo": "super@adbrand.pe"})
        self.assertEqual(fila["accion"], "CAMBIAR_ROL")
        self.assertEqual(fila["entidad"], "usuario_admin")
        self.assertEqual(fila["entidad_id"], self.objetivo.id)
        self.assertIn("FINANZAS", fila["detalle"])

    def test_filtra_por_administrador(self):
        respuesta = self.client.get(f"{URL}?admin_id={self.moderador.id}")

        self.assertEqual(self.ids(respuesta), [self.intermedio.id])

    def test_filtra_desde_una_fecha(self):
        respuesta = self.client.get(f"{URL}?desde={self.dia(5)}")

        self.assertEqual(self.ids(respuesta), [self.reciente.id, self.intermedio.id])

    def test_filtra_hasta_una_fecha(self):
        respuesta = self.client.get(f"{URL}?hasta={self.dia(5)}")

        self.assertEqual(self.ids(respuesta), [self.intermedio.id, self.antiguo.id])

    def test_filtra_por_rango_de_fechas_y_administrador(self):
        respuesta = self.client.get(f"{URL}?admin_id={self.superadmin.id}&desde={self.dia(6)}&hasta={self.dia(0)}")

        self.assertEqual(self.ids(respuesta), [self.reciente.id])

    def test_un_administrador_sin_acciones_devuelve_lista_vacia(self):
        respuesta = self.client.get(f"{URL}?admin_id={self.objetivo.id}")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data, [])

    def test_una_fecha_con_formato_invalido_responde_400(self):
        respuesta = self.client.get(f"{URL}?desde=10/10/2026")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("desde", respuesta.data)

    def test_una_fecha_que_no_existe_responde_400(self):
        respuesta = self.client.get(f"{URL}?hasta=2026-02-31")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("hasta", respuesta.data)

    def test_desde_mayor_que_hasta_responde_400(self):
        respuesta = self.client.get(f"{URL}?desde={self.dia(1)}&hasta={self.dia(5)}")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("desde", respuesta.data)

    def test_un_administrador_invalido_responde_400(self):
        respuesta = self.client.get(f"{URL}?admin_id=abc")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("admin_id", respuesta.data)
