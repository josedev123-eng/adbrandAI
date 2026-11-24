from datetime import timedelta

from django.core import mail
from django.utils import timezone
from rest_framework.test import APITestCase

from auditoria.models import AuditoriaAdmin
from core.pruebas import crear_admin, iniciar_sesion
from moderacion.models import Contenido
from suscripciones.models import Suscripcion
from tokens.models import ConsumoToken

from .models import Empresa

URL = "/api/empresas/"


def crear_empresa(id, nombre_empresa, correo, estado=Empresa.ACTIVO):
    return Empresa.objects.create(
        id=id, nombre="Dueño", nombre_empresa=nombre_empresa, correo=correo, estado=estado,
        fecha_creacion=timezone.now(),
    )


def crear_contenido(usuario_id, estado):
    ahora = timezone.now()
    return Contenido.objects.create(
        usuario_id=usuario_id, tipo="ANUNCIO", red_social="INSTAGRAM", tono="CERCANO", oferta="Oferta",
        texto="Texto", estado=estado, fecha_creacion=ahora, fecha_actualizacion=ahora,
    )


class ListarEmpresasTests(APITestCase):
    """El administrador ve las PYMEs con lo que ayuda a detectar abusos."""

    @classmethod
    def setUpTestData(cls):
        cls.rosa = crear_empresa(1, "Panadería Doña Rosa", "rosa@panaderia.pe")
        cls.humo = crear_empresa(2, "Tabaquería Humo", "humo@tienda.pe")
        crear_contenido(2, Contenido.RECHAZADO)
        crear_contenido(2, Contenido.RECHAZADO)
        crear_contenido(1, Contenido.APROBADO)
        ConsumoToken.objects.create(usuario_id=1, origen="ANUNCIO", tokens_total=1200, fecha=timezone.now())
        hoy = timezone.localdate()
        Suscripcion.objects.create(
            usuario_id=1, cliente="Panadería", plan="Pro", monto=59.90, estado=Suscripcion.ACTIVA,
            fecha_inicio=hoy, fecha_vencimiento=hoy + timedelta(days=20),
        )

    def setUp(self):
        iniciar_sesion(self.client, crear_admin("MODERADOR"))

    def test_lista_con_plan_consumo_y_rechazos_y_primero_las_que_mas_rechazos_tienen(self):
        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual([fila["nombre_empresa"] for fila in respuesta.data], ["Tabaquería Humo", "Panadería Doña Rosa"])
        self.assertEqual(respuesta.data[0]["rechazados"], 2)
        self.assertEqual(respuesta.data[1]["plan"], "Pro")
        self.assertEqual(respuesta.data[1]["tokens_mes"], 1200)

    def test_busca_por_nombre_o_correo(self):
        respuesta = self.client.get(URL, {"busqueda": "panaderia"})

        self.assertEqual([fila["id"] for fila in respuesta.data], [1])

    def test_el_tecnico_no_puede_ver_las_empresas(self):
        iniciar_sesion(self.client, crear_admin("TECNICO"))

        self.assertEqual(self.client.get(URL).status_code, 403)


class SuspenderEmpresaTests(APITestCase):
    """Suspender bloquea a la empresa (Spring lo hace cumplir), queda en la auditoría y le llega un correo."""

    @classmethod
    def setUpTestData(cls):
        cls.superadmin = crear_admin("SUPERADMIN")

    def setUp(self):
        self.empresa = crear_empresa(5, "Tabaquería Humo", "humo@tienda.pe")
        iniciar_sesion(self.client, self.superadmin)

    def suspender(self, motivo="Publica contenido de tabaco una y otra vez."):
        return self.client.post(f"{URL}{self.empresa.id}/suspender/", {"motivo": motivo}, format="json")

    def test_suspender_guarda_el_motivo_registra_en_auditoria_y_avisa_por_correo(self):
        respuesta = self.suspender()

        self.assertEqual(respuesta.status_code, 200)
        self.empresa.refresh_from_db()
        self.assertEqual(self.empresa.estado, Empresa.INACTIVO)
        self.assertEqual(self.empresa.motivo_suspension, "Publica contenido de tabaco una y otra vez.")
        self.assertIsNotNone(self.empresa.fecha_suspension)
        self.assertTrue(AuditoriaAdmin.objects.filter(accion="SUSPENDER_EMPRESA", entidad_id=self.empresa.id).exists())
        self.assertEqual(mail.outbox[0].to, ["humo@tienda.pe"])
        self.assertIn("Motivo: Publica contenido de tabaco", mail.outbox[0].body)

    def test_sin_motivo_o_muy_corto_no_se_suspende(self):
        self.assertEqual(self.suspender("").status_code, 400)
        self.assertEqual(self.suspender("Porque sí").status_code, 400)

        self.empresa.refresh_from_db()
        self.assertEqual(self.empresa.estado, Empresa.ACTIVO)

    def test_reactivar_quita_la_suspension(self):
        self.suspender()

        respuesta = self.client.post(f"{URL}{self.empresa.id}/reactivar/")

        self.assertEqual(respuesta.status_code, 200)
        self.empresa.refresh_from_db()
        self.assertEqual(self.empresa.estado, Empresa.ACTIVO)
        self.assertIsNone(self.empresa.motivo_suspension)
        self.assertTrue(AuditoriaAdmin.objects.filter(accion="REACTIVAR_EMPRESA").exists())
        self.assertEqual(mail.outbox[-1].subject, "Tu cuenta de AdBrand.AI fue reactivada")

    def test_no_se_suspende_dos_veces(self):
        self.suspender()

        self.assertEqual(self.suspender().status_code, 400)
