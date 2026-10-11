from django.test import TestCase

# Create your tests here.

from datetime import timedelta
from decimal import Decimal

from django.utils import timezone
from rest_framework.test import APITestCase

from suscripciones.models import Suscripcion

URL = "/api/suscripciones/con-deuda/"


class SuscripcionesConDeudaTests(APITestCase):
    """HU-08: qué suscripciones devuelve la pantalla de morosos y con qué filtro."""

    @classmethod
    def setUpTestData(cls):
        hoy = timezone.localdate()

        cls.vencida = Suscripcion.objects.create(
            usuario_id=1, cliente="Panadería Doña Rosa", plan="BASICO", monto=Decimal("49.90"),
            estado=Suscripcion.VENCIDA,
            fecha_inicio=hoy - timedelta(days=60), fecha_vencimiento=hoy - timedelta(days=30),
        )
        cls.pendiente = Suscripcion.objects.create(
            usuario_id=2, cliente="Tienda Don Pedro", plan="PRO", monto=Decimal("99.90"),
            estado=Suscripcion.PENDIENTE_PAGO,
            fecha_inicio=hoy - timedelta(days=10), fecha_vencimiento=hoy + timedelta(days=20),
        )
        # Está ACTIVA pero su fecha ya pasó: cuenta como vencida.
        cls.activa_vencida = Suscripcion.objects.create(
            usuario_id=3, cliente="Barbería El Corte", plan="BASICO", monto=Decimal("49.90"),
            estado=Suscripcion.ACTIVA,
            fecha_inicio=hoy - timedelta(days=40), fecha_vencimiento=hoy - timedelta(days=1),
        )
        # ACTIVA y al día: no debe aparecer.
        cls.activa_al_dia = Suscripcion.objects.create(
            usuario_id=4, cliente="Café Central", plan="PRO", monto=Decimal("99.90"),
            estado=Suscripcion.ACTIVA,
            fecha_inicio=hoy - timedelta(days=5), fecha_vencimiento=hoy + timedelta(days=25),
        )
        cls.cancelada = Suscripcion.objects.create(
            usuario_id=5, cliente="Librería Cerrada", plan="BASICO", monto=Decimal("49.90"),
            estado=Suscripcion.CANCELADA,
            fecha_inicio=hoy - timedelta(days=90), fecha_vencimiento=hoy - timedelta(days=60),
        )

    def ids(self, respuesta):
        return [fila["id"] for fila in respuesta.data]

    def test_detecta_las_vencidas(self):
        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 200)
        self.assertIn(self.vencida.id, self.ids(respuesta))

    def test_detecta_las_pendientes_de_pago(self):
        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 200)
        self.assertIn(self.pendiente.id, self.ids(respuesta))

    def test_activa_con_fecha_pasada_cuenta_como_vencida(self):
        respuesta = self.client.get(f"{URL}?estado=VENCIDA")

        self.assertIn(self.activa_vencida.id, self.ids(respuesta))

    def test_activa_al_dia_no_aparece(self):
        respuesta = self.client.get(URL)

        self.assertNotIn(self.activa_al_dia.id, self.ids(respuesta))
        self.assertNotIn(self.cancelada.id, self.ids(respuesta))

    def test_el_filtro_por_estado_separa_vencidas_y_pendientes(self):
        vencidas = self.client.get(f"{URL}?estado=VENCIDA")
        pendientes = self.client.get(f"{URL}?estado=PENDIENTE_PAGO")

        self.assertIn(self.vencida.id, self.ids(vencidas))
        self.assertIn(self.activa_vencida.id, self.ids(vencidas))
        self.assertNotIn(self.pendiente.id, self.ids(vencidas))

        self.assertEqual(self.ids(pendientes), [self.pendiente.id])

    def test_las_fechas_vencidas_llegan_primero(self):
        respuesta = self.client.get(URL)

        fechas = [fila["fecha_vencimiento"] for fila in respuesta.data]
        self.assertEqual(fechas, sorted(fechas))

    def test_un_estado_invalido_responde_400(self):
        respuesta = self.client.get(f"{URL}?estado=CANCELADA")

        self.assertEqual(respuesta.status_code, 400)


URL_ACTIVAS = "/api/suscripciones/activas/"


def crear_suscripcion(estado, vence_en_dias, cliente="Panadería Doña Rosa"):
    hoy = timezone.localdate()
    return Suscripcion.objects.create(
        usuario_id=1, cliente=cliente, plan="BASICO", monto=Decimal("49.90"), estado=estado,
        fecha_inicio=hoy - timedelta(days=30), fecha_vencimiento=hoy + timedelta(days=vence_en_dias),
    )


class SuscripcionesActivasTests(APITestCase):
    """HU-05: cuántas PYMEs están usando el servicio hoy."""

    def total(self):
        respuesta = self.client.get(URL_ACTIVAS)

        self.assertEqual(respuesta.status_code, 200)
        return respuesta.data["total"]

    def test_sin_suscripciones_el_total_es_cero(self):
        self.assertEqual(self.total(), 0)

    def test_una_activa_vigente_cuenta(self):
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=25)

        self.assertEqual(self.total(), 1)

    def test_una_activa_que_vence_hoy_todavia_cuenta(self):
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=0)

        self.assertEqual(self.total(), 1)

    def test_una_activa_con_fecha_pasada_no_cuenta(self):
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=-1)

        self.assertEqual(self.total(), 0)

    def test_vencida_pendiente_y_cancelada_no_cuentan(self):
        crear_suscripcion(Suscripcion.VENCIDA, vence_en_dias=-30)
        crear_suscripcion(Suscripcion.PENDIENTE_PAGO, vence_en_dias=20)
        crear_suscripcion(Suscripcion.CANCELADA, vence_en_dias=20)

        self.assertEqual(self.total(), 0)

    def test_cuenta_solo_las_activas_vigentes_entre_varias(self):
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=25, cliente="Café Central")
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=3, cliente="Tienda Don Pedro")
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=-5, cliente="Barbería El Corte")
        crear_suscripcion(Suscripcion.VENCIDA, vence_en_dias=-40, cliente="Librería Cerrada")
        crear_suscripcion(Suscripcion.PENDIENTE_PAGO, vence_en_dias=10, cliente="Bodega San Martín")

        self.assertEqual(self.total(), 2)

    def test_la_respuesta_solo_trae_el_total(self):
        crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=25)

        respuesta = self.client.get(URL_ACTIVAS)

        self.assertEqual(respuesta.data, {"total": 1})
