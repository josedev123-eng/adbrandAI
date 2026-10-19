from datetime import date, timedelta

from django.utils import timezone
from rest_framework.test import APITestCase

from core.pruebas import con_sesion, crear_admin, iniciar_sesion
from moderacion.models import PerfilNegocio

from .models import ConsumoMensual, ConsumoToken, LimiteTokens
from .services import inicio_del_mes_actual, limite_de_empresa

URL = "/api/tokens/consumo-actual/"


def consumir(usuario_id, tokens, fecha=None, origen="ANUNCIO"):
    return ConsumoToken.objects.create(
        usuario_id=usuario_id, origen=origen, modelo="Qwen/Qwen3", tokens_entrada=tokens // 2,
        tokens_salida=tokens - tokens // 2, tokens_total=tokens, fecha=fecha or timezone.now(),
    )


class ConsumoActualTests(APITestCase):
    """HU 17: tokens de IA consumidos por cada empresa en el mes actual."""

    @classmethod
    def setUpTestData(cls):
        cls.tecnico = crear_admin("TECNICO")
        PerfilNegocio.objects.create(usuario_id=1, nombre_comercial="Panadería Doña Rosa", rubro="Panadería")
        PerfilNegocio.objects.create(usuario_id=2, nombre_comercial="Café Central", rubro="Cafetería")

    def setUp(self):
        iniciar_sesion(self.client, self.tecnico)

    def consultar(self):
        respuesta = self.client.get(URL)
        self.assertEqual(respuesta.status_code, 200)
        return respuesta.data

    def test_sin_consumo_el_total_es_cero(self):
        datos = self.consultar()

        self.assertEqual(datos["mes"], timezone.localdate().strftime("%Y-%m"))
        self.assertEqual(datos["total"], 0)
        self.assertEqual(datos["empresas"], [])

    def test_suma_los_tokens_de_cada_empresa(self):
        consumir(1, 100)
        consumir(1, 50, origen="REGENERAR_ANUNCIO")
        consumir(2, 30, origen="KIT_MARCA")

        datos = self.consultar()

        self.assertEqual(datos["total"], 180)
        rosa = datos["empresas"][0]
        self.assertEqual(rosa["usuario_id"], 1)
        self.assertEqual(rosa["tokens"], 150)
        self.assertEqual(rosa["generaciones"], 2)

    def test_ordena_de_la_empresa_que_mas_gasta_a_la_que_menos(self):
        consumir(1, 20)
        consumir(2, 500)

        empresas = self.consultar()["empresas"]

        self.assertEqual([fila["usuario_id"] for fila in empresas], [2, 1])

    def test_no_cuenta_los_meses_anteriores(self):
        consumir(1, 100)
        consumir(1, 999, fecha=inicio_del_mes_actual() - timedelta(minutes=1))

        datos = self.consultar()

        self.assertEqual(datos["total"], 100)
        self.assertEqual(datos["empresas"][0]["generaciones"], 1)

    def test_el_nombre_sale_del_perfil_o_es_empresa_con_su_id(self):
        consumir(2, 10)
        consumir(7, 10)

        nombres = {fila["usuario_id"]: fila["empresa"] for fila in self.consultar()["empresas"]}

        self.assertEqual(nombres[2], "Café Central")
        self.assertEqual(nombres[7], "Empresa #7")

    def test_trae_la_fecha_de_la_ultima_generacion(self):
        consumir(1, 10, fecha=timezone.now() - timedelta(seconds=30))
        reciente = consumir(1, 10)

        fila = self.consultar()["empresas"][0]

        self.assertEqual(fila["ultima_generacion"], timezone.localtime(reciente.fecha).isoformat())

    def test_cada_generacion_nueva_se_ve_al_volver_a_consultar(self):
        # HU 17, criterio 2: el panel se actualiza sin volver a iniciar sesión.
        consumir(1, 100)
        self.assertEqual(self.consultar()["total"], 100)

        consumir(1, 40)

        self.assertEqual(self.consultar()["total"], 140)


class ConsumoActualPermisosTests(APITestCase):
    """HU 17: solo el administrador técnico y el superadministrador ven el consumo."""

    def test_tecnico_y_superadmin_entran(self):
        for rol in ["TECNICO", "SUPERADMIN"]:
            with self.subTest(rol=rol):
                respuesta = self.client.get(URL, **con_sesion(crear_admin(rol)))
                self.assertEqual(respuesta.status_code, 200)

    def test_moderador_y_finanzas_reciben_403(self):
        for rol in ["MODERADOR", "FINANZAS"]:
            with self.subTest(rol=rol):
                respuesta = self.client.get(URL, **con_sesion(crear_admin(rol)))
                self.assertEqual(respuesta.status_code, 403)
                self.assertEqual(respuesta.data["detail"], "Tu rol no tiene acceso a esta sección.")

    def test_sin_sesion_pide_iniciar_sesion(self):
        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 403)
        self.assertEqual(respuesta.data["detail"], "Inicia sesión para continuar.")


class AlertaConsumoTests(APITestCase):
    """HU 19: alerta cuando una empresa llega al porcentaje de aviso de su límite mensual.

    En la base de prueba limite_tokens empieza vacía (la fila por defecto de V31 no se carga),
    así que cada prueba pone el límite que necesita.
    """

    @classmethod
    def setUpTestData(cls):
        cls.tecnico = crear_admin("TECNICO")

    def setUp(self):
        iniciar_sesion(self.client, self.tecnico)

    def limite(self, limite_mensual, porcentaje_aviso=80, usuario_id=None):
        return LimiteTokens.objects.create(
            usuario_id=usuario_id, limite_mensual=limite_mensual, porcentaje_aviso=porcentaje_aviso
        )

    def consultar(self):
        respuesta = self.client.get(URL)
        self.assertEqual(respuesta.status_code, 200)
        return respuesta.data

    def fila(self, datos, usuario_id):
        return next(fila for fila in datos["empresas"] if fila["usuario_id"] == usuario_id)

    def test_al_79_por_ciento_no_hay_alerta(self):
        # Criterio 2: por debajo del 80 % no hay ninguna alerta.
        self.limite(1000)
        consumir(1, 790)

        datos = self.consultar()

        self.assertEqual(datos["alertas"], [])
        fila = self.fila(datos, 1)
        self.assertFalse(fila["alerta"])
        self.assertEqual(fila["porcentaje"], 79.0)
        self.assertEqual(fila["limite"], 1000)

    def test_justo_al_80_por_ciento_hay_alerta(self):
        # Criterio 1: la alerta trae la empresa y el porcentaje consumido.
        PerfilNegocio.objects.create(usuario_id=1, nombre_comercial="Panadería Doña Rosa", rubro="Panadería")
        self.limite(1000)
        consumir(1, 500)
        consumir(1, 300)

        datos = self.consultar()

        self.assertEqual(
            datos["alertas"],
            [{"usuario_id": 1, "empresa": "Panadería Doña Rosa", "tokens": 800, "limite": 1000, "porcentaje": 80.0}],
        )
        self.assertTrue(self.fila(datos, 1)["alerta"])

    def test_mas_del_100_por_ciento_sigue_en_alerta(self):
        self.limite(1000)
        consumir(1, 1234)

        alerta = self.consultar()["alertas"][0]

        self.assertEqual(alerta["porcentaje"], 123.4)

    def test_solo_avisa_de_las_empresas_que_llegaron_y_de_mayor_a_menor(self):
        self.limite(1000)
        consumir(1, 850)
        consumir(2, 100)
        consumir(3, 990)

        alertas = self.consultar()["alertas"]

        self.assertEqual([alerta["usuario_id"] for alerta in alertas], [3, 1])

    def test_el_limite_propio_de_la_empresa_gana_al_de_por_defecto(self):
        self.limite(1000)
        self.limite(5000, usuario_id=1)
        consumir(1, 900)  # 90 % del de por defecto, pero solo 18 % del suyo
        consumir(2, 900)

        datos = self.consultar()

        self.assertEqual(self.fila(datos, 1)["limite"], 5000)
        self.assertEqual(self.fila(datos, 1)["porcentaje"], 18.0)
        self.assertEqual([alerta["usuario_id"] for alerta in datos["alertas"]], [2])
        self.assertEqual(limite_de_empresa(1).limite_mensual, 5000)
        self.assertEqual(limite_de_empresa(2).limite_mensual, 1000)

    def test_respeta_un_porcentaje_de_aviso_distinto_de_80(self):
        self.limite(1000, porcentaje_aviso=50)
        consumir(1, 500)
        consumir(2, 499)

        datos = self.consultar()

        self.assertEqual([alerta["usuario_id"] for alerta in datos["alertas"]], [1])
        self.assertEqual(self.fila(datos, 1)["porcentaje_aviso"], 50)

    def test_sin_ningun_limite_no_hay_alertas_ni_error(self):
        consumir(1, 999999)

        datos = self.consultar()

        self.assertEqual(datos["alertas"], [])
        fila = self.fila(datos, 1)
        self.assertIsNone(fila["limite"])
        self.assertIsNone(fila["porcentaje"])
        self.assertFalse(fila["alerta"])
        self.assertIsNone(limite_de_empresa(1))

    def test_el_moderador_no_ve_las_alertas(self):
        self.limite(1000)
        consumir(1, 900)

        iniciar_sesion(self.client, crear_admin("MODERADOR"))

        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 403)
        self.assertNotIn("alertas", respuesta.data)


URL_HISTORIAL = "/api/tokens/historial/"


class HistorialTests(APITestCase):
    """HU 20: tokens consumidos en cada mes de un periodo y el total.

    En la base de prueba la vista vista_consumo_token_mensual (V32) es una tabla común,
    así que se llena directo con lo que la vista devolvería.
    """

    @classmethod
    def setUpTestData(cls):
        cls.tecnico = crear_admin("TECNICO")
        for mes, usuario_id, tokens, generaciones in [
            (date(2026, 8, 1), 1, 1000, 4),
            (date(2026, 8, 1), 2, 500, 1),
            (date(2026, 10, 1), 1, 300, 2),
            (date(2026, 7, 1), 1, 9999, 9),  # fuera del periodo
        ]:
            ConsumoMensual.objects.create(mes=mes, usuario_id=usuario_id, tokens=tokens, generaciones=generaciones)

    def setUp(self):
        iniciar_sesion(self.client, self.tecnico)

    def test_muestra_cada_mes_del_periodo_y_el_total(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-08&hasta=2026-10")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["desde"], "2026-08")
        self.assertEqual(respuesta.data["hasta"], "2026-10")
        self.assertEqual(respuesta.data["total"], 1800)
        self.assertEqual(
            respuesta.data["meses"],
            [
                {"mes": "2026-08", "tokens": 1500, "generaciones": 5},
                {"mes": "2026-09", "tokens": 0, "generaciones": 0},  # un mes sin consumo sale en cero
                {"mes": "2026-10", "tokens": 300, "generaciones": 2},
            ],
        )

    def test_un_periodo_de_un_solo_mes(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-10&hasta=2026-10")

        self.assertEqual(respuesta.data["total"], 300)
        self.assertEqual(len(respuesta.data["meses"]), 1)

    def test_filtra_por_una_empresa(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-08&hasta=2026-10&usuario_id=2")

        self.assertEqual(respuesta.data["total"], 500)
        self.assertEqual(respuesta.data["meses"][0]["tokens"], 500)

    def test_un_periodo_que_cruza_de_anio(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-11&hasta=2027-02")

        self.assertEqual([mes["mes"] for mes in respuesta.data["meses"]], ["2026-11", "2026-12", "2027-01", "2027-02"])
        self.assertEqual(respuesta.data["total"], 0)

    def test_inicio_posterior_al_fin_responde_400(self):
        # HU 20, criterio 2
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-10&hasta=2026-08")

        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(respuesta.data["desde"][0], "La fecha de inicio no puede ser posterior a la fecha de fin")

    def test_formato_invalido_responde_400(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=08/2026&hasta=2026-13")

        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(respuesta.data["desde"][0], "Usa el formato AAAA-MM.")
        self.assertIn("hasta", respuesta.data)

    def test_faltan_las_fechas_responde_400(self):
        respuesta = self.client.get(URL_HISTORIAL)

        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(respuesta.data["desde"][0], "Indica el mes de inicio.")
        self.assertEqual(respuesta.data["hasta"][0], "Indica el mes de fin.")

    def test_un_periodo_de_mas_de_24_meses_responde_400(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2024-01&hasta=2026-01")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("hasta", respuesta.data)

    def test_el_moderador_recibe_403(self):
        iniciar_sesion(self.client, crear_admin("MODERADOR"))

        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-08&hasta=2026-10")

        self.assertEqual(respuesta.status_code, 403)
