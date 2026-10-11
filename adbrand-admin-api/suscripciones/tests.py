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


from datetime import date

from suscripciones.models import Pago

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


URL_INGRESOS = "/api/suscripciones/ingresos-mensuales/"


def primer_dia_del_mes(meses_atras):
    """Primer día del mes que está `meses_atras` meses antes del actual."""
    hoy = timezone.localdate()
    total = hoy.year * 12 + (hoy.month - 1) - meses_atras
    return hoy.replace(year=total // 12, month=total % 12 + 1, day=1)


def nombre_del_mes(meses_atras):
    return primer_dia_del_mes(meses_atras).strftime("%Y-%m")


class IngresosMensualesTests(APITestCase):
    """HU-06: cuánto se cobró en cada uno de los últimos 6 meses."""

    @classmethod
    def setUpTestData(cls):
        cls.suscripcion = crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=25)

    def pagar(self, monto, meses_atras, dia=1, metodo=Pago.YAPE):
        fecha = primer_dia_del_mes(meses_atras) + timedelta(days=dia - 1)
        return Pago.objects.create(
            suscripcion=self.suscripcion, monto=Decimal(monto), fecha_pago=fecha, metodo=metodo,
        )

    def ingresos(self):
        respuesta = self.client.get(URL_INGRESOS)

        self.assertEqual(respuesta.status_code, 200)
        return {fila["mes"]: fila["total"] for fila in respuesta.data}

    def test_devuelve_los_ultimos_seis_meses_en_orden_contando_el_actual(self):
        respuesta = self.client.get(URL_INGRESOS)

        self.assertEqual(respuesta.status_code, 200)
        meses = [fila["mes"] for fila in respuesta.data]
        self.assertEqual(meses, [nombre_del_mes(atras) for atras in (5, 4, 3, 2, 1, 0)])

    def test_sin_pagos_todos_los_meses_salen_en_cero(self):
        self.assertEqual(set(self.ingresos().values()), {"0.00"})

    def test_suma_los_pagos_de_cada_mes(self):
        self.pagar("49.90", meses_atras=2, dia=3)
        self.pagar("99.90", meses_atras=2, dia=15, metodo=Pago.TARJETA)
        self.pagar("49.90", meses_atras=0)

        ingresos = self.ingresos()

        self.assertEqual(ingresos[nombre_del_mes(2)], "149.80")
        self.assertEqual(ingresos[nombre_del_mes(0)], "49.90")

    def test_un_mes_sin_pagos_entre_dos_con_pagos_sale_en_cero(self):
        self.pagar("49.90", meses_atras=3)
        self.pagar("49.90", meses_atras=1)

        ingresos = self.ingresos()

        self.assertEqual(ingresos[nombre_del_mes(3)], "49.90")
        self.assertEqual(ingresos[nombre_del_mes(2)], "0.00")
        self.assertEqual(ingresos[nombre_del_mes(1)], "49.90")

    def test_cuenta_el_primer_y_el_ultimo_dia_del_mes_en_ese_mes(self):
        self.pagar("10.00", meses_atras=1, dia=1)
        ultimo_dia = primer_dia_del_mes(0) - timedelta(days=1)
        Pago.objects.create(
            suscripcion=self.suscripcion, monto=Decimal("20.00"), fecha_pago=ultimo_dia, metodo=Pago.YAPE,
        )

        ingresos = self.ingresos()

        self.assertEqual(ingresos[nombre_del_mes(1)], "30.00")
        self.assertEqual(ingresos[nombre_del_mes(0)], "0.00")

    def test_un_pago_de_hace_mas_de_seis_meses_no_aparece(self):
        self.pagar("500.00", meses_atras=6)
        self.pagar("49.90", meses_atras=5)

        ingresos = self.ingresos()

        self.assertEqual(len(ingresos), 6)
        self.assertNotIn(nombre_del_mes(6), ingresos)
        self.assertEqual(ingresos[nombre_del_mes(5)], "49.90")

    def test_el_ingreso_es_el_pago_y_no_el_monto_de_la_suscripcion(self):
        # La suscripción cuesta 49.90 pero este mes solo se cobró un pago de 20.00.
        self.pagar("20.00", meses_atras=0)

        self.assertEqual(self.ingresos()[nombre_del_mes(0)], "20.00")


class IngresosPorRangoTests(APITestCase):
    """HU-07: filtrar los ingresos por un rango de meses para comparar periodos."""

    @classmethod
    def setUpTestData(cls):
        cls.suscripcion = crear_suscripcion(Suscripcion.ACTIVA, vence_en_dias=25)
        # Un pago por mes en un año fijo, para que el rango no dependa de la fecha de hoy.
        for mes, monto in [(1, "10.00"), (2, "20.00"), (3, "30.00"), (5, "50.00")]:
            Pago.objects.create(
                suscripcion=cls.suscripcion, monto=Decimal(monto),
                fecha_pago=date(2025, mes, 15), metodo=Pago.YAPE,
            )

    def rango(self, desde, hasta):
        return self.client.get(f"{URL_INGRESOS}?desde={desde}&hasta={hasta}")

    def test_devuelve_solo_los_meses_del_rango_con_los_dos_extremos(self):
        respuesta = self.rango("2025-02", "2025-04")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            [dict(fila) for fila in respuesta.data],
            [
                {"mes": "2025-02", "total": "20.00"},
                {"mes": "2025-03", "total": "30.00"},
                {"mes": "2025-04", "total": "0.00"},
            ],
        )

    def test_un_rango_de_un_solo_mes_devuelve_ese_mes(self):
        respuesta = self.rango("2025-05", "2025-05")

        self.assertEqual([dict(fila) for fila in respuesta.data], [{"mes": "2025-05", "total": "50.00"}])

    def test_un_rango_que_cruza_de_anio_sigue_el_orden_de_los_meses(self):
        respuesta = self.rango("2024-11", "2025-02")

        self.assertEqual([fila["mes"] for fila in respuesta.data], ["2024-11", "2024-12", "2025-01", "2025-02"])
        self.assertEqual([fila["total"] for fila in respuesta.data], ["0.00", "0.00", "10.00", "20.00"])

    def test_dos_periodos_distintos_dan_totales_distintos(self):
        primer_trimestre = self.rango("2025-01", "2025-03")
        segundo_trimestre = self.rango("2025-04", "2025-06")

        self.assertEqual(sum(Decimal(fila["total"]) for fila in primer_trimestre.data), Decimal("60.00"))
        self.assertEqual(sum(Decimal(fila["total"]) for fila in segundo_trimestre.data), Decimal("50.00"))

    def test_un_rango_de_24_meses_es_valido(self):
        respuesta = self.rango("2024-01", "2025-12")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(len(respuesta.data), 24)

    def test_un_rango_de_mas_de_24_meses_responde_400(self):
        respuesta = self.rango("2024-01", "2026-01")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("hasta", respuesta.data)

    def test_desde_mayor_que_hasta_responde_400(self):
        respuesta = self.rango("2025-06", "2025-01")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("desde", respuesta.data)

    def test_un_formato_invalido_responde_400(self):
        for desde in ["2025", "2025-13", "2025-01-15", "enero", "01-2025"]:
            with self.subTest(desde=desde):
                respuesta = self.rango(desde, "2025-06")

                self.assertEqual(respuesta.status_code, 400)
                self.assertIn("desde", respuesta.data)

    def test_solo_uno_de_los_dos_meses_responde_400(self):
        solo_desde = self.client.get(f"{URL_INGRESOS}?desde=2025-01")
        solo_hasta = self.client.get(f"{URL_INGRESOS}?hasta=2025-06")

        self.assertEqual(solo_desde.status_code, 400)
        self.assertIn("hasta", solo_desde.data)
        self.assertEqual(solo_hasta.status_code, 400)
        self.assertIn("desde", solo_hasta.data)

    def test_sin_rango_siguen_saliendo_los_ultimos_seis_meses(self):
        respuesta = self.client.get(URL_INGRESOS)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            [fila["mes"] for fila in respuesta.data],
            [nombre_del_mes(atras) for atras in (5, 4, 3, 2, 1, 0)],
        )
